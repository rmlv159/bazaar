package com.r.bazaar;

import com.r.bazaar.entity.model.Field;
import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.persist.DAO.AuxRelationRow;
import com.r.bazaar.output.persist.PostgreSQLDAO.GenericTableRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

//TODO RK impl visitor pattern?
@Component
public class MatchingAlgo {

    private final DAO dao;

    private final MatchingRepo matchingRepo;
    private final Map<String, Set<String>> choiceTypes;
    private static final List<String> AUX_RELATIONS_FIELD_NAMES = List.of(
        "system_default_table_name",
        "systemDefaultFieldName",
        "value",
        "aux_relation_target_system_default",
        "auxRelationSourceGeneric",
        "cache_1to1_generic",
        "cache_fact_generic");

    public MatchingAlgo(
        //injection with qualifier on the fields directly didn't work, strangely, despite it
        // working everywhere else
        @Qualifier("dao")
        DAO dao,
        @Autowired
        MatchingRepo matchingRepo,
        @Qualifier("choiceTypes")
        Map<String, Set<String>> choiceTypes) {
        this.dao = dao;
        this.matchingRepo = matchingRepo;
        this.choiceTypes = choiceTypes;
    }

    //    protected record Submission(
    protected class InsertionPreparedMatch {

        List<String> genericFieldNames;
        List<String> data;
        List<String> cacheType;
        List<String> cacheLocation;
        //TODO instead we need a queue here
    }

    //TODO RK genericTableNames should be unique/Set
    protected Map<String, InsertionPreparedMatch> filter(int clientId, String backupId,
        String systemName,
        List<String> genericTableNames, ArrayList<Field> schema, ArrayList<String> data,
        String systemDefaultTableName) {

        //TODO maybe implement calls directly as query to build logic?
//        matchingRepo.refresh();

        //TODO RK this is a hack, company code should be removed by replacing company code from
        // looking up back up id. Or is it fine anyway?
        systemDefaultTableName = systemDefaultTableName.split("\\$")[1].replace("]", "");

        Map<String, GenericTableRow> systemDefaultFieldsToGenericFields = dao.getSystemDefaultToGeneric(
            clientId, systemName, genericTableNames);
        Map<String, Set<String>> genericTablesToGenericFields = dao.getGenericTablesToGenericFields(
            clientId, systemName, genericTableNames);

        //TODO RK not needed for now?
//        Map<String, String> genericFieldsToSystemDefaultFields = dao.getGenericFieldsToSystemDefaultFields(
//            clientId, systemName, genericTableNames);

        Map<String, InsertionPreparedMatch> insertions = new HashMap<>();

        for (Map.Entry<String, Set<String>> genericTableEntry : genericTablesToGenericFields.entrySet()) {
            for (int i = 0; i < data.size(); i++) {
                String systemDefaultFieldName = schema.get(i).getName();
                String qualifiedSystemDefaultField =
                    systemDefaultTableName + "." + systemDefaultFieldName;
                //TODO maybe write a test
                //TODO debug more
                GenericTableRow row = systemDefaultFieldsToGenericFields.get(
                    qualifiedSystemDefaultField);
                if (null == row) {
                    continue;
                }
                String value = data.get(i);
                String genericFieldName = systemDefaultFieldsToGenericFields.get(
                    qualifiedSystemDefaultField).genericFieldName();
                if (!genericTableEntry.getValue().contains(genericFieldName)) {
                    continue;
                }

                //TODO bad, new objects
                var insertion = insertions.getOrDefault(genericTableEntry.getKey(),
                    new InsertionPreparedMatch());
                if (null == insertion.genericFieldNames) {
                    insertion.genericFieldNames = new ArrayList<>();
                    insertion.data = new ArrayList<>();
                    insertion.cacheType = new ArrayList<>();
                    insertion.cacheLocation = new ArrayList<>();
                }
                fillMasterDataRelationsRetain(clientId, systemName, genericTableEntry,
                    systemDefaultFieldName,
                    insertion);

                insertion.genericFieldNames.add(genericFieldName);
                insertion.data.add(value);
                fillCacheInfo(row, insertion);

                insertions.put(genericTableEntry.getKey(), insertion);
            }
        }

        for (var insertion : insertions.entrySet()) {
            for (int i = 0; i < insertion.getValue().genericFieldNames.size(); i++) {
//                String genericFieldName = insertion.getValue().filteredGenericFieldNames.get(i);
//                String value = insertion.getValue().data.get(i);
                String cacheType = insertion.getValue().cacheType.get(i);
                String cacheLocation = insertion.getValue().cacheLocation.get(i);
                if (null == cacheType) {
                    continue;
                }
                if (cacheType.equals("1to1")) {
                    put1To1(cacheLocation, insertion.getKey(), insertions);
                }
            }
        }
        return insertions;
    }

    private void fillMasterDataRelationsRetain(int clientId, String systemName,
        Entry<String, Set<String>> genericTableEntry, String systemDefaultFieldName,
        InsertionPreparedMatch insertion) {
        if (genericTableEntry.getKey().equals("master_data_relations_retain")) {
            //TODO RK do something here that makes more sense
            if (systemName.equals("NAV18")) {
                if (systemDefaultFieldName.equals("LCY Code")) {
                    String companyCode = dao.getCompanyCode(clientId);
                    insertion.genericFieldNames.add("key");
                    insertion.data.add(companyCode);
                    insertion.cacheType.add("");
                    insertion.cacheLocation.add("");
                }
            }
        }
    }

    protected void processAuxRelation(AuxRelationRow row) {
        List<String> genericTableNames = dao.getGenericTableNames(row.systemDefaultTableName(),
            row.systemName());
        Map<String, GenericTableRow> systemDefaultFieldsToGenericFields = dao.getSystemDefaultToGeneric(
            row.clientId(), row.systemName(), genericTableNames);
        Map<String, Set<String>> genericTablesToGenericFields = dao.getGenericTablesToGenericFields(
            row.clientId(), row.systemName(), genericTableNames);

        String systemDefaultTableName = row.systemDefaultTableName();
        String systemDefaultFieldName = row.systemDefaultFieldName();
        String qualifiedSystemDefaultField = systemDefaultTableName + "." + systemDefaultFieldName;

        //VAT Entry
        //aux_relation_target_system_default
        if (null != row.auxRelationTargetSystemDefault()) {
            LinkedList<String> idChain = new LinkedList<>();
            idChain.add(row.value());
        GenericTableRow originalGenericRow = systemDefaultFieldsToGenericFields.get(
            qualifiedSystemDefaultField);
            processAuxTargetingSystemDefaults(row, idChain, originalGenericRow,
                systemDefaultFieldsToGenericFields);
        }

        dao.removeAuxRelationRow(row);
    }

    private void processAuxTargetingSystemDefaults(AuxRelationRow row, LinkedList<String> idChain,
        GenericTableRow originalGenericRow, Map<String, GenericTableRow> systemDefaultFieldsToGenericFields) {
        String[] systemDefaults = row.auxRelationTargetSystemDefault().split("\\.");
        //G_L Entry - VAT Entry Link	G_L Entry No_
        AuxRelationRow relation = dao.getOneAuxRelationRow(row.clientId(), row.backupId(),
            AUX_RELATIONS_FIELD_NAMES, systemDefaults[0], systemDefaults[1], row.value());

        //TODO can I actually better do this in sql?
        idChain.add(relation.value());
        if (null != relation.auxRelationTargetSystemDefault()) {
            processAuxTargetingSystemDefaults(relation, idChain, originalGenericRow,
                systemDefaultFieldsToGenericFields);
        }
        dao.updateGenericTable(originalGenericRow, relation., idChain.getFirst(), idChain.getLast());
    }

    private static void fillCacheInfo(GenericTableRow row, InsertionPreparedMatch insertion) {
        //TODO RK make nicer
        if (null != row.auxRelationTargetSystemDefault()) {
            insertion.cacheType.add("aux_relation_target_system_default");
            insertion.cacheLocation.add(row.auxRelationTargetSystemDefault());
        } else if (null != row.cache1To1Generic()) {
            insertion.cacheType.add("1to1");
            insertion.cacheLocation.add(row.cache1To1Generic());
        } else if (null != row.cacheFactGeneric()) {
            insertion.cacheType.add("fact");
            insertion.cacheLocation.add(row.cacheFactGeneric());
        } else {
            insertion.cacheType.add("");
            insertion.cacheLocation.add("");
        }
    }

    private void put1To1(String cacheLocationGenericTableName, String genericTableName,
        Map<String, InsertionPreparedMatch> insertions) {

        InsertionPreparedMatch oneToOneOuterInsertion = insertions.get(genericTableName);

        if (isChoice(genericTableName)) {
            //TODO RK continue with choosing the right amount
            List<Field> schema = dao.getSchema(genericTableName);
            Set<Field> choices = schema.stream()
                .filter(f -> f.getName().endsWith("_choice"))
                .collect(Collectors.toSet());
            if (isAmountStruct(choices)) {
                handleDebitCredit(cacheLocationGenericTableName, genericTableName, insertions,
                    oneToOneOuterInsertion);
            }
        }
        insertions.remove(genericTableName);
    }

    private void handleDebitCredit(String cacheLocationGenericTableName, String genericTableName,
        Map<String, InsertionPreparedMatch> insertions,
        InsertionPreparedMatch oneToOneOuterInsertion) {
        int DEBIT_AND_CREDIT = 2;
        long id1To1 = 0;
        for (int i = 0; i < DEBIT_AND_CREDIT - 1; i++) {
            if (!oneToOneOuterInsertion.genericFieldNames.get(i)
                .equals("amount")) {
                throw new RuntimeException(
                    "This method is supposed to capture debit and credit, if the "
                        + "oneToOneOuterInsertion.filteredGenericFieldNames doesn't "
                        + "contain an amount field, something is wrong (however, "
                        + "this may only be true for NAV18).");
            }
            String amountStr = oneToOneOuterInsertion.data.get(i);
            if (Double.parseDouble(amountStr)
                != 0.0) { //TODO RK implement floating point imprecision check?
                //TODO build some logic further upstream, where system_default_field and
                // generic_field are next to each other, that detects when the debit field
                // does not come before the credit field (should be the natural NAV 18 column
                // order
                id1To1 = dao.putAndGetId(genericTableName,
                    List.of(oneToOneOuterInsertion.genericFieldNames.get(i), "debit_choice"),
                    List.of(amountStr,
                        "yes")); //TODO RK remove yes after it's decided what kind of binary data type in the DB should be used
                break;
            } else {
                id1To1 = dao.putAndGetId(genericTableName,
                    List.of(oneToOneOuterInsertion.genericFieldNames.get(i + 1), "credit_choice"),
                    List.of(oneToOneOuterInsertion.data.get(i + 1), "yes"));
            }
        }
        insertions.get(cacheLocationGenericTableName).genericFieldNames.add(
            genericTableName + "_1to1_id");
        insertions.get(cacheLocationGenericTableName).data.add(String.valueOf(id1To1));
        //for consistency
        insertions.get(cacheLocationGenericTableName).cacheType.add("");
        insertions.get(cacheLocationGenericTableName).cacheLocation.add("");
    }

    private boolean isAmountStruct(Set<Field> choices) {
        return choices.stream()
            .filter(f -> f.getName().equals("debit"))
            .findFirst()
            .isEmpty();
    }

    private boolean isChoice(String genericTableName) {
        return choiceTypes.containsKey(genericTableName);
    }
}
