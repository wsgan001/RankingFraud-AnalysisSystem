package Ranking;

import Controller.DataController;
import DataModel.AppCluster;
import DataModel.AppData;
import ToolKit.Print;
import com.google.common.collect.Sets;

import java.util.*;

/**
 * Created by chenhao on 3/27/16.
 */

public class RankingAnalysis {
    public TreeMap<String, AppCluster> rankGroupMap = new TreeMap<>();
    public DataController dataController;
    private List<AppCluster> groupList = new LinkedList<>();
    private TreeMap<Date, Set<AppCluster>> endDayMap = new TreeMap<>();
    private TreeMap<Date, Set<AppCluster>> beginDayMap = new TreeMap<>();

    public RankingAnalysis(DataController dataController) {
        this.dataController = dataController;
        dataController.getRankAppInfoFromDb().buildAppDataMapForRank();

    }

    public static void main(String args[]) {
        DataController dataController = new DataController();
        RankingAnalysis rankingAnalysis = new RankingAnalysis(dataController);
        rankingAnalysis.rankGroupMapGenerate();
        System.out.println("合并前Group数: " + rankingAnalysis.rankGroupMap.size());
        double rate = 0.8;
        rankingAnalysis.mapRecursiveCombine(rate, rankingAnalysis.rankGroupMap);
        System.out.println("合并后Group数: " + rankingAnalysis.rankGroupMap.size());
        Print.printEachGroupSize(rankingAnalysis.rankGroupMap);
    }

    public TreeMap<String, AppCluster> getRankGroupMap() {
        return rankGroupMap;
    }

    public void getGroupByRank() {
        findUpDownPattern();
        findDownUpPattern();
        findUpUpPattern();
        findDownDownPattern();
        beginEndMapBuilder(beginDayMap, endDayMap);
        expandGroup(beginDayMap, endDayMap);
    }

    private Set<String> getIntersectionIdSet(Set<String> setA, Set<String> setB) {
        Set<String> tmpSet = new HashSet<>();
        for (String appId : setA) {
            if (setB.contains(appId))
                tmpSet.add(appId);
        }
        return tmpSet;
    }

    private Set<Date> getDateIntersectionSet(Set<Date> setA, Set<Date> setB) {
        Set<Date> tmpSet = new HashSet<>();
        for (Date date : setA) {
            if (setB.contains(date))
                tmpSet.add(date);
        }

        if (tmpSet.isEmpty())
            return null;
        else
            return tmpSet;
    }

    //return day difference date1-date2
    private int dayDiff(Date date1, Date date2) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        int day1 = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTime(date2);
        int day2 = calendar.get(Calendar.DAY_OF_YEAR);

        return day1 - day2;
    }

    public List<AppCluster> getGroupList() {
        return groupList;
    }


    //include same day up and down
    //insert new group object into the groupList
    private void findUpDownPattern() {
        //free up and down try catch
        Iterator freeDownIter;
        Iterator freeUpIter = dataController.getFreeUpMap().entrySet().iterator();
        while (freeUpIter.hasNext()) {
            Map.Entry upEntry = (Map.Entry) freeUpIter.next();
            Set<String> upSet = (HashSet) upEntry.getValue();
            Date upDate = (Date) upEntry.getKey();

            freeDownIter = dataController.getFreeDownMap().entrySet().iterator();
            while (freeDownIter.hasNext()) {
                Map.Entry downEntry = (Map.Entry) freeDownIter.next();
                Set<String> downSet = (HashSet) downEntry.getValue();
                Date downDate = (Date) downEntry.getKey();
                Set<String> interSet = getIntersectionIdSet(upSet, downSet);
                if (!interSet.isEmpty()) {
                    int dayDiff = dayDiff(downDate, upDate);
                    String groupType = "FreeUpDown";
                    AppCluster group = new AppCluster(groupType, dayDiff, interSet);
                    group.setDate(upDate, downDate);
                    groupList.add(group);
                }
            }
        }

        //paid up and down try catch
        Iterator paidDownIter;
        Iterator paidUpIter = dataController.getPaidUpMap().entrySet().iterator();
        while (paidUpIter.hasNext()) {
            Map.Entry upEntry = (Map.Entry) paidUpIter.next();
            Set<String> upSet = (HashSet) upEntry.getValue();
            Date upDate = (Date) upEntry.getKey();

            paidDownIter = dataController.getPaidDownMap().entrySet().iterator();
            while (paidDownIter.hasNext()) {
                Map.Entry downEntry = (Map.Entry) paidDownIter.next();
                Set<String> downSet = (HashSet) downEntry.getValue();
                Date downDate = (Date) downEntry.getKey();
                Set<String> interSet = getIntersectionIdSet(upSet, downSet);
                if (!interSet.isEmpty()) {
                    int dayDiff = dayDiff(downDate, upDate);
                    String groupType = "PaidUpDown";
                    AppCluster group = new AppCluster(groupType, dayDiff, interSet);
                    group.setDate(upDate, downDate);
                    groupList.add(group);
                }
            }
        }
    }

    //exclude same day up down
    private void findDownUpPattern() {
        //free down and up try catch
        Iterator freeUpIter;
        Iterator freeDownIter = dataController.getFreeDownMap().entrySet().iterator();
        while (freeDownIter.hasNext()) {
            Map.Entry downEntry = (Map.Entry) freeDownIter.next();
            Set<String> downSet = (HashSet) downEntry.getValue();
            Date downDate = (Date) downEntry.getKey();
            freeUpIter = dataController.getFreeUpMap().entrySet().iterator();
            while (freeUpIter.hasNext()) {
                Map.Entry upEntry = (Map.Entry) freeUpIter.next();
                Set<String> upSet = (HashSet) upEntry.getValue();
                Date upDate = (Date) upEntry.getKey();
                Set<String> interSet = getIntersectionIdSet(downSet, upSet);
                if (!interSet.isEmpty()) {
                    int dayDiff = dayDiff(upDate, downDate);
                    String groupType = "FreeDownUp";
                    AppCluster group = new AppCluster(groupType, dayDiff, interSet);
                    group.setDate(downDate, upDate);
                    groupList.add(group);
                }
            }
        }

        //paid down and up try catch
        Iterator paidUpIter;
        Iterator paidDownIter = dataController.getPaidDownMap().entrySet().iterator();
        while (paidDownIter.hasNext()) {
            Map.Entry downEntry = (Map.Entry) paidDownIter.next();
            Set<String> downSet = (HashSet) downEntry.getValue();
            Date downDate = (Date) downEntry.getKey();
            paidUpIter = dataController.getPaidUpMap().entrySet().iterator();

            // pass one
            paidUpIter.next();
            while (paidUpIter.hasNext()) {
                Map.Entry upEntry = (Map.Entry) paidUpIter.next();
                Set<String> upSet = (HashSet) upEntry.getValue();
                Date upDate = (Date) upEntry.getKey();
                Set<String> interSet = getIntersectionIdSet(downSet, upSet);
                if (!interSet.isEmpty()) {
                    int dayDiff = dayDiff(upDate, downDate);
                    String groupType = "PaidDownUp";
                    AppCluster group = new AppCluster(groupType, dayDiff, interSet);
                    group.setDate(downDate, upDate);
                    groupList.add(group);
                }
            }
        }
    }

    private void findUpUpPattern() {
        //free up up
        Iterator freeUpNextIter;
        Iterator freeUpIter = dataController.getFreeUpMap().entrySet().iterator();
        while (freeUpIter.hasNext()) {
            Map.Entry upEntry = (Map.Entry) freeUpIter.next();
            Set<String> upSet = (HashSet) upEntry.getValue();
            Date upDate = (Date) upEntry.getKey();

            freeUpNextIter = dataController.getFreeUpMap().entrySet().iterator();
            //pass one
            freeUpNextIter.next();
            while (freeUpNextIter.hasNext()) {
                Map.Entry upNextEntry = (Map.Entry) freeUpNextIter.next();
                Set<String> upNextSet = (HashSet) upNextEntry.getValue();
                Date upDateNext = (Date) upNextEntry.getKey();
                Set<String> interSet = getIntersectionIdSet(upSet, upNextSet);
                if (!interSet.isEmpty()) {
                    int dayDiff = dayDiff(upDateNext, upDate);
                    String groupType = "FreeUpUp";
                    AppCluster group = new AppCluster(groupType, dayDiff, interSet);
                    group.setDate(upDate, upDateNext);
                    groupList.add(group);
                }
            }
        }

        //paid up and  up catch
        Iterator paidUpNextIter;
        Iterator paidUpIter = dataController.getPaidUpMap().entrySet().iterator();
        while (paidUpIter.hasNext()) {
            Map.Entry upEntry = (Map.Entry) paidUpIter.next();
            Set<String> upSet = (HashSet) upEntry.getValue();
            Date upDate = (Date) upEntry.getKey();
            paidUpNextIter = dataController.getPaidUpMap().entrySet().iterator();
            //pass next
            paidUpNextIter.next();
            while (paidUpNextIter.hasNext()) {
                Map.Entry upNextEntry = (Map.Entry) paidUpNextIter.next();
                Set<String> upNextSet = (HashSet) upNextEntry.getValue();
                Date upDateNext = (Date) upNextEntry.getKey();
                Set<String> interSet = getIntersectionIdSet(upSet, upNextSet);
                if (!interSet.isEmpty()) {
                    int dayDiff = dayDiff(upDateNext, upDate);
                    String groupType = "PaidUpUp";
                    AppCluster group = new AppCluster(groupType, dayDiff, interSet);
                    group.setDate(upDate, upDateNext);
                    groupList.add(group);
                }
            }
        }
    }

    private void findDownDownPattern() {
        //free up up
        Iterator freeDownNextIter;
        Iterator freeDownIter = dataController.getFreeDownMap().entrySet().iterator();
        while (freeDownIter.hasNext()) {
            Map.Entry downEntry = (Map.Entry) freeDownIter.next();
            Set<String> downSet = (HashSet) downEntry.getValue();
            Date downDate = (Date) downEntry.getKey();

            freeDownNextIter = dataController.getFreeDownMap().entrySet().iterator();
            //pass one
            freeDownNextIter.next();
            while (freeDownNextIter.hasNext()) {
                Map.Entry downNextEntry = (Map.Entry) freeDownNextIter.next();
                Set<String> downNextSet = (HashSet) downNextEntry.getValue();
                Date downDateNext = (Date) downNextEntry.getKey();
                Set<String> interSet = getIntersectionIdSet(downSet, downNextSet);
                if (!interSet.isEmpty()) {
                    int dayDiff = dayDiff(downDateNext, downDate);
                    String groupType = "FreeUpUp";
                    AppCluster group = new AppCluster(groupType, dayDiff, interSet);
                    group.setDate(downDate, downDateNext);
                    groupList.add(group);
                }
            }
        }

        //paid up and  up catch
        Iterator paidDownNextIter;
        Iterator paidDownIter = dataController.getPaidDownMap().entrySet().iterator();
        while (paidDownIter.hasNext()) {
            Map.Entry downEntry = (Map.Entry) paidDownIter.next();
            Set<String> downSet = (HashSet) downEntry.getValue();
            Date downDate = (Date) downEntry.getKey();

            paidDownNextIter = dataController.getPaidDownMap().entrySet().iterator();
            paidDownNextIter.next();
            while (paidDownNextIter.hasNext()) {
                Map.Entry downNextEntry = (Map.Entry) paidDownNextIter.next();
                Set<String> downNextSet = (HashSet) downNextEntry.getValue();
                Date downDateNext = (Date) downNextEntry.getKey();
                Set<String> interSet = getIntersectionIdSet(downSet, downNextSet);
                if (!interSet.isEmpty()) {
                    int dayDiff = dayDiff(downDateNext, downDate);
                    String groupType = "PaidDownDown";
                    AppCluster group = new AppCluster(groupType, dayDiff, interSet);
                    group.setDate(downDate, downDateNext);
                    groupList.add(group);
                }
            }
        }
    }

    //use group list to generate two map, the begin day map and the end day map.
    public void beginEndMapBuilder(Map<Date, Set<AppCluster>> beginDayMap, Map<Date, Set<AppCluster>> endDayMap) {

        //remove duplicate data and build tree
        Iterator iterator = groupList.iterator();
        while (iterator.hasNext()) {
            AppCluster group = (AppCluster) iterator.next();
            if (group.dateDiffNum < 0 || ((group.groupType.equals("FreeDownUp") || group.groupType.equals("PaidDownUp")) && group.dateDiffNum == 0)) {
                iterator.remove();
            } else {

                if (beginDayMap.containsKey(group.getBeginDate())) {
                    beginDayMap.get(group.getBeginDate()).add(group);
                } else {
                    Set<AppCluster> newSet = new HashSet<>();
                    newSet.add(group);
                    beginDayMap.put(group.getBeginDate(), newSet);
                }

                if (endDayMap.containsKey(group.getEndDate())) {
                    endDayMap.get(group.getEndDate()).add(group);
                } else {
                    Set<AppCluster> newSet = new HashSet<>();
                    newSet.add(group);
                    endDayMap.put(group.getEndDate(), newSet);
                }
            }
        }
    }

    public void beginEndMapBuilder(List<AppCluster> list, Map<Date, Set<AppCluster>> beginDayMap, Map<Date, Set<AppCluster>> endDayMap) {
        //remove duplicate data and build tree
        if (list.isEmpty())
            return;

        //clear the original elements
        beginDayMap.clear();
        endDayMap.clear();

        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            AppCluster group = (AppCluster) iterator.next();

            if (beginDayMap.containsKey(group.getBeginDate())) {
                beginDayMap.get(group.getBeginDate()).add(group);
            } else {
                Set<AppCluster> newSet = new HashSet<>();
                newSet.add(group);
                beginDayMap.put(group.getBeginDate(), newSet);
            }

            if (endDayMap.containsKey(group.getEndDate())) {
                endDayMap.get(group.getEndDate()).add(group);
            } else {
                Set<AppCluster> newSet = new HashSet<>();
                newSet.add(group);
                endDayMap.put(group.getEndDate(), newSet);
            }
        }
    }


    // return the intersection of endGroup and beginGroup, return null if intersect nothing
    private AppCluster getCombineGroup(AppCluster endGroup, AppCluster beginGroup) {
        AppCluster mixedGroup = new AppCluster();
        Set<String> intersectionSet = getIntersectionIdSet(endGroup.getAppIdSet(), beginGroup.getAppIdSet());

        if (intersectionSet.isEmpty())
            return null;

        mixedGroup.setAppIdSet(intersectionSet);
        mixedGroup.setDate(endGroup.getBeginDate(), beginGroup.getEndDate());
        mixedGroup.dateDiffNum = endGroup.dateDiffNum + beginGroup.dateDiffNum;
        return mixedGroup;
    }

    //combine the group with same end day and begin day
    private void expandGroup(Map<Date, Set<AppCluster>> entryBeginDayMap, Map<Date, Set<AppCluster>> entryEndDayMap) {
        Iterator beginDatIter;
        Iterator endDatIter = entryEndDayMap.entrySet().iterator();
        List<AppCluster> tmpGroupList = new LinkedList<>();
        endDatePlusLoop:
        while (endDatIter.hasNext()) {
            beginDatIter = entryBeginDayMap.entrySet().iterator();
            Map.Entry endEntry = (Map.Entry) endDatIter.next();
            Date endDate = (Date) endEntry.getKey();

            Map.Entry beginEntry;
            Date beginDate;
            if (beginDatIter.hasNext()) {

                beginDatIter.next();
                beginEntry = (Map.Entry) beginDatIter.next();
                beginDate = (Date) beginEntry.getKey();

                while (!beginDate.equals(endDate)) {
                    if (beginDatIter.hasNext()) {
                        beginDatIter.next();
                        beginEntry = (Map.Entry) beginDatIter.next();
                        beginDate = (Date) beginEntry.getKey();
                    } else {
                        continue endDatePlusLoop;
                    }
                }
            } else {
                continue;
            }

            Set<AppCluster> beginGroupSet = (Set) beginEntry.getValue();
            Set<AppCluster> endGroupSet = (Set) endEntry.getValue();

            Object[] beginGroupArray = beginGroupSet.toArray();
            Object[] endGroupArray = endGroupSet.toArray();

            tmpGroupList = new LinkedList<>();
            for (int i = 0; i < beginGroupArray.length; i++) {
                for (int j = i; j < endGroupArray.length; j++) {
                    AppCluster beginGroup = (AppCluster) beginGroupArray[i];
                    AppCluster endGroup = (AppCluster) endGroupArray[j];
                    AppCluster combinedGroup = getCombineGroup(endGroup, beginGroup);
                    if (combinedGroup != null)
                        tmpGroupList.add(combinedGroup);
                }
            }
        }
        beginEndMapBuilder(tmpGroupList, entryBeginDayMap, entryEndDayMap);
    }

    public void expandToAll(Map<Date, Set<AppCluster>> entryBeginDayMap, Map<Date, Set<AppCluster>> entryEndDayMap) {

        while (true) {
            Set<Date> endDateSet = entryEndDayMap.keySet();
            Set<Date> beginDateSet = entryBeginDayMap.keySet();
            if (getDateIntersectionSet(endDateSet, beginDateSet) == null)
                return;
            expandGroup(entryBeginDayMap, entryEndDayMap);
        }
    }

    public void rankPatternCombine(List<AppData> outerAppList, String outerAppId, List<AppData> innerAppList, String innerAppId) {

        int duplicateCount = 0;
        Set<Date> dateSet = new HashSet<>();

        for (int i = 0; i < outerAppList.size(); i++) {
            for (int j = i; j < innerAppList.size(); j++) {
                AppData appA = outerAppList.get(i);
                AppData appB = innerAppList.get(j);
                dateSet.add(appA.date);
                dateSet.add(appB.date);
                if (appA.rankType.equals(appB.rankType) && appA.date.equals(appB.date))
                    duplicateCount++;
            }
        }

        if (duplicateCount >= dataController.RANK_MIN_NUM) {
            if (rankGroupMap.containsKey(outerAppId)) {
                AppCluster appCluster = rankGroupMap.get(outerAppId);
                appCluster.getAppIdSet().add(innerAppId);
                appCluster.commonChangeDateSet.addAll(dateSet);
            } else {
                AppCluster newGroup = new AppCluster();
                newGroup.getAppIdSet().add(outerAppId);
                newGroup.getAppIdSet().add(innerAppId);
                newGroup.commonChangeDateSet.addAll(dateSet);
                rankGroupMap.put(outerAppId, newGroup);
            }
        }
    }

    public void rankGroupMapGenerateTest(int minimum) {
        Map appRankMap = dataController.getAppMapForRank();
        Object[] outerAppRankArray = appRankMap.entrySet().toArray();
        Object[] innerAppRankArray = outerAppRankArray.clone();

        for (int i = 0; i < outerAppRankArray.length; i++) {
            for (int j = i + 1; j < innerAppRankArray.length; j++) {
                Map.Entry outerEntry = (Map.Entry) outerAppRankArray[i];
                Map.Entry innerEntry = (Map.Entry) innerAppRankArray[j];

                String outerId = outerEntry.getKey().toString();
                String innerId = innerEntry.getKey().toString();

                List outerList = (List) outerEntry.getValue();
                List innerList = (List) innerEntry.getValue();
                rankPatternCombineTest(outerList, outerId, innerList, innerId, minimum);
            }
        }
    }


    public void rankPatternCombineTest(List<AppData> outerAppList, String outerAppId, List<AppData> innerAppList, String innerAppId, int minimum) {

        int duplicateCount = 0;
        Set<Date> dateSet = new HashSet<>();

        for (int i = 0; i < outerAppList.size(); i++) {
            for (int j = i; j < innerAppList.size(); j++) {
                AppData appA = outerAppList.get(i);
                AppData appB = innerAppList.get(j);
                dateSet.add(appA.date);
                dateSet.add(appB.date);
                if (appA.rankType.equals(appB.rankType) && appA.date.equals(appB.date))
                    duplicateCount++;
            }
        }
        if (duplicateCount >= minimum) {
            if (rankGroupMap.containsKey(outerAppId)) {
                AppCluster appCluster = rankGroupMap.get(outerAppId);
                appCluster.getAppIdSet().add(innerAppId);
                appCluster.commonChangeDateSet.addAll(dateSet);
            } else {
                AppCluster newGroup = new AppCluster();
                newGroup.getAppIdSet().add(outerAppId);
                newGroup.getAppIdSet().add(innerAppId);
                newGroup.commonChangeDateSet.addAll(dateSet);
                rankGroupMap.put(outerAppId, newGroup);
            }
        }
    }

    //generate the rankGroupMap that has appId as key, and AppCluster as element
    public void rankGroupMapGenerate() {
        Map appRankMap = dataController.getAppMapForRank();
        Object[] outerAppRankArray = appRankMap.entrySet().toArray();
        Object[] innerAppRankArray = outerAppRankArray.clone();

        for (int i = 0; i < outerAppRankArray.length; i++) {
            for (int j = i + 1; j < innerAppRankArray.length; j++) {
                Map.Entry outerEntry = (Map.Entry) outerAppRankArray[i];
                Map.Entry innerEntry = (Map.Entry) innerAppRankArray[j];

                String outerId = outerEntry.getKey().toString();
                String innerId = innerEntry.getKey().toString();

                List outerList = (List) outerEntry.getValue();
                List innerList = (List) innerEntry.getValue();
                rankPatternCombine(outerList, outerId, innerList, innerId);
            }
        }
    }


    //combine the generated app rank map, to remove the subset of some entry
    public void mapRecursiveCombine(double rate) {

        boolean hasDuplicateSet = false;
        //Object[] outerRankGroupArray = rankGroupMap.entrySet().toArray();
        //Object[] innerRankGroupArray = rankGroupMap.entrySet().toArray();
        Object[] outerIdSet = rankGroupMap.keySet().toArray();
        Object[] innerIdSet = rankGroupMap.keySet().toArray();

        for (int i = 0; i < outerIdSet.length; i++) {
            for (int j = i + 1; j < innerIdSet.length; j++) {

                // Map.Entry outerEntry = (Map.Entry) outerRankGroupArray[i];
                // Map.Entry innerEntry = (Map.Entry) innerRankGroupArray[j];

                // String outerId = outerEntry.getKey().toString();
                // String innerId = innerEntry.getKey().toString();

                String outerId = outerIdSet[i].toString();
                String innerId = innerIdSet[j].toString();

                //AppCluster outerRankingGroup = (AppCluster) outerEntry.getValue();
                // AppCluster innerRankingGroup = (AppCluster) innerEntry.getValue();

                Set<String> outerSet;
                Set<String> innerSet;
                if (rankGroupMap.containsKey(outerId) && rankGroupMap.containsKey(innerId)) {
                    outerSet = rankGroupMap.get(outerId).getAppIdSet();
                    innerSet = rankGroupMap.get(innerId).getAppIdSet();

//                if (outerSet == null || innerSet == null)
//                    continue;

                    //int outerGroupSize = outerRankingGroup.getAppSize();
                    //int innerGroupSize = innerRankingGroup.getAppSize();

                    int outerGroupSize = outerSet.size();
                    int innerGroupSize = innerSet.size();

//                if (outerRankingGroup.getAppIdSet().containsAll(innerRankingGroup.getAppIdSet())
//                        || innerRankingGroup.getAppIdSet().containsAll(outerRankingGroup.getAppIdSet())
//                        || enableCombine(innerRankingGroup.getAppIdSet(), outerRankingGroup.getAppIdSet(), rate))
                    if (outerSet.containsAll(innerSet)
                            || innerSet.containsAll(outerSet)
                            || enableCombine(innerSet, outerSet, rate)) {
                        if (outerGroupSize > innerGroupSize) {
                            //if (rankGroupMap.get(innerId) != null && rankGroupMap.get(outerId) != null)
                            //rankGroupMap.get(outerId).getAppIdSet().addAll(rankGroupMap.get(innerId).getAppIdSet());
                            outerSet.addAll(innerSet);
                            rankGroupMap.remove(innerId);

                        } else {
                            //if (rankGroupMap.get(outerId) != null && rankGroupMap.get(innerId) != null)
                            //rankGroupMap.get(innerId).getAppIdSet().addAll(rankGroupMap.get(outerId).getAppIdSet());
                            innerSet.addAll(outerSet);
                            rankGroupMap.remove(outerId);
                        }
                        hasDuplicateSet = true;
                    }
                }
            }
        }

        if (hasDuplicateSet)
            mapRecursiveCombine(rate);
    }

    public void mapRecursiveCombine(double rate, Map<String, AppCluster> groupMap) {
        boolean hasDuplicateSet = false;
        Object[] outerIdSet = groupMap.keySet().toArray();
        Object[] innerIdSet = groupMap.keySet().toArray();

        for (int i = 0; i < outerIdSet.length; i++) {
            for (int j = i + 1; j < innerIdSet.length; j++) {
                String outerId = outerIdSet[i].toString();
                String innerId = innerIdSet[j].toString();

                Set<String> outerSet;
                Set<String> innerSet;
                if (groupMap.containsKey(outerId) && groupMap.containsKey(innerId)) {
                    outerSet = groupMap.get(outerId).getAppIdSet();
                    innerSet = groupMap.get(innerId).getAppIdSet();

                    int outerGroupSize = outerSet.size();
                    int innerGroupSize = innerSet.size();

                    if (outerSet.containsAll(innerSet)
                            || innerSet.containsAll(outerSet)
                            || enableCombine(innerSet, outerSet, rate)) {
                        if (outerGroupSize > innerGroupSize) {
                            outerSet.addAll(innerSet);
                            groupMap.remove(innerId);

                        } else {
                            innerSet.addAll(outerSet);
                            groupMap.remove(outerId);
                        }
                        hasDuplicateSet = true;
                    }
                }
            }
        }

        if (hasDuplicateSet)
            mapRecursiveCombine(rate, rankGroupMap);
    }

    private boolean enableCombine(Set<String> setA, Set<String> setB, double rate) {
        Set<String> unionSet = Sets.union(setA, setB);
        Set<String> intersectionSet = Sets.intersection(setA, setB);
        double unionSize = unionSet.size();
        double intersectionSize = intersectionSet.size();
        return (intersectionSize / unionSize) >= rate;
    }

    public void generateExportDate() {
        Iterator iterator = rankGroupMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            AppCluster group = (AppCluster) entry.getValue();
            if (group.getAppSize() > 20) {
                Set set = group.getAppIdSet();
                Object[] array = set.toArray();
                String idA = array[3].toString();
                String idB = array[4].toString();
                String idC = array[5].toString();
                Map map = dataController.getAppMapForRank();
                List<AppData> listA = (List<AppData>) map.get(idA);
                List<AppData> listB = (List<AppData>) map.get(idB);
                List<AppData> listC = (List<AppData>) map.get(idC);
                int appA;
                int appB;
                int appC;
                Set<Date> dataSet = group.commonChangeDateSet;
                for (Date date : dataSet) {
                    appA = getDateAppData(date, listA);
                    appB = getDateAppData(date, listB);
                    appC = getDateAppData(date, listC);
                    dataController.insertTestDataToDb(date, appA, appB, appC, 0, 0, 0);
                }
                return;
            }
        }
    }

    private int getDateAppData(Date date, List<AppData> list) {
        for (AppData app : list) {
            if (app.date.equals(date)) {
                return app.ranking;
            }
        }
        return 0;
    }


}
