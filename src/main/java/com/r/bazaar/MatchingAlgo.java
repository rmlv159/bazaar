package com.r.bazaar;

import com.r.bazaar.entity.model.Field;
import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.persist.PostgreSQLDAO.Row;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        List<String> filteredGenericFieldNames;
        List<String> data;
        List<String> cacheType;
        List<String> cacheLocation;
        //TODO instead we need a queue here
    }

    //TODO RK genericTableNames should be unique/Set
    protected Map<String, InsertionPreparedMatch> filter(int clientId, String systemName,
        List<String> genericTableNames, ArrayList<Field> schema, ArrayList<String> data,
        String systemDefaultTableName) {

        //TODO maybe implement calls directly as query to build logic?
//        matchingRepo.refresh();

        //TODO RK this is a hack, company code should be removed by replacing company code from
        // looking up back up id. Or is it fine anyway?
        systemDefaultTableName = systemDefaultTableName.split("\\$")[1].replace("]", "");

        Map<String, Row> systemDefaultFieldsToGenericFields = dao.getSystemDefaultToGeneric(
            clientId, systemName, genericTableNames);
        Map<String, Set<String>> genericTablesToGenericFields = dao.getGenericTablesToGenericFields(
            clientId, systemName, genericTableNames);

        //TODO RK not needed for now?
//        Map<String, String> genericFieldsToSystemDefaultFields = dao.getGenericFieldsToSystemDefaultFields(
//            clientId, systemName, genericTableNames);

        Map<String, InsertionPreparedMatch> insertions = new HashMap<>();

        for (Map.Entry<String, Set<String>> genericTableEntry : genericTablesToGenericFields.entrySet()) {
//            List<String> sortedGenericFieldNames = new ArrayList<>();
//            List<String> sortedData = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                String systemDefaultFieldName = schema.get(i).getName();
                String qualifiedSystemDefaultField =
                    systemDefaultTableName + "." + systemDefaultFieldName;
                //TODO maybe write a test
                //TODO debug more
//                if (!matchingRepo.containsSystemDefaultField(qualifiedSystemDefaultField, clientId, systemName)) {
                Row row = systemDefaultFieldsToGenericFields.get(qualifiedSystemDefaultField);
                if (null == row) {
                    continue;
                }
                String value = data.get(i);
                String genericFieldName = systemDefaultFieldsToGenericFields.get(
                    qualifiedSystemDefaultField).genericFieldName();
                if (!genericTableEntry.getValue().contains(genericFieldName)) {
                    continue;
                }

//                sortedGenericFieldNames.add(genericFieldName);
//                sortedData.add(value);
//                String genericTableName = genericTableEntry.getKey();genericFieldsToSystemDefaultFields.get(genericFieldName))
                //TODO bad, new objects
                var insertion = insertions.getOrDefault(genericTableEntry.getKey(),
                    new InsertionPreparedMatch());
                if (null == insertion.filteredGenericFieldNames) {
                    insertion.filteredGenericFieldNames = new ArrayList<>();
                    insertion.data = new ArrayList<>();
                    insertion.cacheType = new ArrayList<>();
                    insertion.cacheLocation = new ArrayList<>();
                }
                insertion.filteredGenericFieldNames.add(genericFieldName);
                insertion.data.add(value);

                fillCacheInfo(row, insertion);

                insertions.put(genericTableEntry.getKey(), insertion);
            }
        }

        for (var insertion : insertions.entrySet()) {
            for (int i = 0; i < insertion.getValue().filteredGenericFieldNames.size(); i++) {
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

    private static void fillCacheInfo(Row row, InsertionPreparedMatch insertion) {
        //TODO RK make nicer
        if (null != row.auxRelationTargetsSystemDefault()) {
            insertion.cacheType.add("aux_relation_targets_system_default");
            insertion.cacheLocation.add(row.auxRelationTargetsSystemDefault());
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
            if (!oneToOneOuterInsertion.filteredGenericFieldNames.get(i)
                .equals("amount")) {
                throw new RuntimeException(
                    "This method is supposed to capture debit and credit, if the "
                        + "oneToOneOuterInsertion.filteredGenericFieldNames doesn't "
                        + "contain an amount field, something is wrong (however, "
                        + "this may only be true for NAV18).");
            }
            String amountStr = oneToOneOuterInsertion.data.get(i);
            if (Double.parseDouble(amountStr) != 0.0) { //TODO RK implement floating point imprecision check?
                //TODO build some logic further upstream, where system_default_field and
                // generic_field are next to each other, that detects when the debit field
                // does not come before the credit field (should be the natural NAV 18 column
                // order
                id1To1 = dao.putAndGetId(genericTableName,
                    List.of(oneToOneOuterInsertion.filteredGenericFieldNames.get(i), "debit_choice"),
                    List.of(amountStr, "yes")); //TODO RK remove yes after it's decided what kind of binary data type in the DB should be used
                break;
            } else {
                id1To1 = dao.putAndGetId(genericTableName,
                    //TODO RK is -1 correct now after credit was always 0.0?
                    List.of(oneToOneOuterInsertion.filteredGenericFieldNames.get(i-1), "credit_choice"),
                    List.of(oneToOneOuterInsertion.data.get(i), "yes"));
            }
        }
        insertions.get(cacheLocationGenericTableName).filteredGenericFieldNames.add(
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
