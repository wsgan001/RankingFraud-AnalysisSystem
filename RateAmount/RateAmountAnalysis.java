package RateAmount;

import Controller.DataController;
import DataModel.AppCluster;
import DataModel.AppData;
import DataModel.RateAmountDiffRecord;
import Ranking.RankingAnalysis;
import ToolKit.Combination;
import ToolKit.Print;
import com.google.common.collect.Sets;

import java.util.*;


/**
 * Created by chenhao on 3/30/16.
 */

public class RateAmountAnalysis {
    public Map<String, AppCluster> rateNumGroupMap = new TreeMap<>();
    private DateComparator dateComparator = new DateComparator();
    private DataController dataController;
    //对diffRecordMap数据结构做出修改,改为
    //Map<String, TreeMap<Date,RateAmountDiffRecord>>
    //在进入比较之后, 任意两个APP 的日期记录比较,通过TreeMap的轮训记录进行,而不是穷举
    //取出TreeMap的每天的所有Key值,然后取交集,得到单一Key值
    //或者去元素含量小的一组Key值进行循环遍历
    private Map<String, List<AppData>> appDataMap = new HashMap<>();
    private Map<String, HashMap<Date, RateAmountDiffRecord>> diffRecordMap = new HashMap<>();
    public Map<String, AppData> appMetaDataMap;

    public RateAmountAnalysis(DataController dataController) {
        this.dataController = dataController;
        dataController.buildAppMapForRateNumFromDb();
        appDataMap = dataController.getAppMapForRateNum();
        appMetaDataMap = dataController.getAppMetaDataMapForRateNum();
    }

    public static void main(String args[]) {
        DataController dataController = new DataController();
        RankingAnalysis rankingAnalysis = new RankingAnalysis(dataController);
        RateAmountAnalysis rateAmountAnalysis = new RateAmountAnalysis(dataController);
        rateAmountAnalysis.buildDiffRecordMap();
        rateAmountAnalysis.rateNumGroupMapGenerate();
        System.out.println("----------------------------------------------");
        System.out.println("合并前Group数: " + rateAmountAnalysis.rateNumGroupMap.size());
        double rate = 0.8;
        rateAmountAnalysis.mapRecursiveCombine(rate, rateAmountAnalysis.rateNumGroupMap);
        System.out.println("合并后Group数" + rateAmountAnalysis.rateNumGroupMap.size());
        System.out.println("----------------------------------------------");
        Print.printEachGroupSize(rateAmountAnalysis.rateNumGroupMap);

//        Map metaMap = rateAmountAnalysis.appMetaDataMap;
//        Set set = metaMap.entrySet();
//        Iterator iterator = set.iterator();
//        int count = 0;
//        while (iterator.hasNext()) {
//            Map.Entry entry = (Map.Entry) iterator.next();
//            AppData appData = (AppData) entry.getValue();
//            if (appData.hasNumDecrease)
//                count++;
//        }
//
//        System.out.println("app数" + set.size());
//        System.out.println("评论减少app数" + count);


    }

    //生成评论差值的hash map, key 是app Id, value是存储着每天差值记录rateAmountDiffRecord的集合
    public Map buildDiffRecordMap() {
        Iterator iterator = appDataMap.keySet().iterator();
        HashMap<Date, RateAmountDiffRecord> map;
        while (iterator.hasNext()) {
            String appId = iterator.next().toString();
            map = generateDiffSet(appId);
            diffRecordMap.put(appId, map);
        }
        return diffRecordMap;

    }

    //根据输入的app id 值,得到每天评论的差值集合,比如2号-1号,3号-2号...构成的集合
    private HashMap<Date, RateAmountDiffRecord> generateDiffSet(String appId) {
        HashMap<Date, RateAmountDiffRecord> diffMap = new HashMap<>();
        List<AppData> list = appDataMap.get(appId);
        Collections.sort(list, dateComparator);
        int totalDiffAmount = 0;
        for (int current = 0, next = 1; next < list.size() && current < list.size(); next++, current++) {
            RateAmountDiffRecord record = new RateAmountDiffRecord();
            record.amountDiff = list.get(next).userTotalRateCount - list.get(current).userTotalRateCount;
            //若评论数量呈正增长区属,则计入增长总数中
            //对于评论数量下降的异常情况,考虑苹果删除评论的可能性,则不从已有的累计数量中扣除
            if (record.amountDiff > 0)
                totalDiffAmount += record.amountDiff;
            //若评论数为负增长,对相应app的metadata值标记
            if (record.amountDiff < 0) {
                recordRateNumDecrease(appId);
            }
            record.date = list.get(next).date;
            record.appId = appId;
            diffMap.put(record.date, record);
        }
        double avgNum = (double) totalDiffAmount / (double) list.size();
        appMetaDataMap.get(appId).averageDailyRateNum = avgNum;
        return diffMap;
    }

    //在app data list里找到指定的app,并标记其内的isRateNumDecrease值
    private void recordRateNumDecrease(String appId) {
        appMetaDataMap.get(appId).hasNumDecrease = true;
    }

    public void rateNumGroupMapGenerate() {

        Object[] outerArray = diffRecordMap.entrySet().toArray().clone();
        Object[] innerArray = diffRecordMap.entrySet().toArray().clone();

        for (int i = 0; i < outerArray.length; i++) {
            for (int j = i + 1; j < innerArray.length; j++) {

                Map.Entry outerEntry = (Map.Entry) outerArray[i];
                Map.Entry innerEntry = (Map.Entry) innerArray[j];

                String outerId = outerEntry.getKey().toString();
                String innerId = innerEntry.getKey().toString();

                HashMap outerMap = (HashMap) outerEntry.getValue();
                HashMap innerMap = (HashMap) innerEntry.getValue();
                quickRateNumDiffPatternCombine(outerMap, outerId, innerMap, innerId);
            }
        }
    }

    public void quickRateNumGroupRankGenerate() {
        Object[] keyArray = diffRecordMap.keySet().toArray();
        int[] num = new int[keyArray.length];
        for (int i = 0; i < keyArray.length; i++) {
            num[i] = i;
        }
        List<int[]> keyPair = Combination.combine(num, 2);
        for (int i = 0; i < keyPair.size(); i++) {
            int[] x = keyPair.get(i);
            String appId1 = keyArray[x[0]].toString();
            String appId2 = keyArray[x[1]].toString();
            System.out.println(i);
            //rateNumDiffPatternCombine(diffRecordMap.get(appId1), appId1, diffRecordMap.get(appId2), appId2);
        }
//
    }

    private int[] getSubset(int[] input, int[] subset) {
        int[] result = new int[subset.length];
        for (int i = 0; i < subset.length; i++)
            result[i] = input[subset[i]];
        return result;
    }

    //此处做出修改
    private void rateNumDiffPatternCombine(List<RateAmountDiffRecord> outerAppList, String outerAppId, List<RateAmountDiffRecord> innerAppList, String innerAppId) {


        double outerAppAvgDiffNum = appMetaDataMap.get(outerAppId).averageDailyRateNum;
        double innerAppAvgDiffNum = appMetaDataMap.get(innerAppId).averageDailyRateNum;

        int duplicateCount = 0;
        for (int i = 0; i < outerAppList.size(); i++) {
            for (int j = i; j < innerAppList.size(); j++) {
                RateAmountDiffRecord outerDiffRecord = outerAppList.get(i);
                RateAmountDiffRecord innerDiffRecord = innerAppList.get(j);
                if (outerDiffRecord.date.equals(innerDiffRecord.date)) {
                    if ((outerDiffRecord.amountDiff > outerAppAvgDiffNum && innerDiffRecord.amountDiff > innerAppAvgDiffNum)
                            || (outerDiffRecord.amountDiff < outerAppAvgDiffNum && innerDiffRecord.amountDiff < innerAppAvgDiffNum
                            && outerDiffRecord.amountDiff > 0 && innerDiffRecord.amountDiff > 0))
                        duplicateCount++;
                }
            }
        }
        if (duplicateCount >= 10) {
            //System.out.println(duplicateCount);
            if (rateNumGroupMap.containsKey(outerAppId)) {
                AppCluster appCluster = rateNumGroupMap.get(outerAppId);
                appCluster.getAppIdSet().add(innerAppId);
            } else {
                AppCluster newGroup = new AppCluster();
                newGroup.getAppIdSet().add(outerAppId);
                newGroup.getAppIdSet().add(innerAppId);
                rateNumGroupMap.put(outerAppId, newGroup);
            }
        }
    }

    private void quickRateNumDiffPatternCombine(
            HashMap<Date, RateAmountDiffRecord> outerMap, String outerAppId,
            HashMap<Date, RateAmountDiffRecord> innerMap, String innerAppId) {
        double outerAppAvgDiffNum = appMetaDataMap.get(outerAppId).averageDailyRateNum;
        double innerAppAvgDiffNum = appMetaDataMap.get(innerAppId).averageDailyRateNum;
        Set<Date> outerDateSet = outerMap.keySet();
        Set<Date> innerDateSet = innerMap.keySet();

        Set<Date> shareDateSet = (Set) Sets.intersection(outerDateSet, innerDateSet);

        int duplicateCount = 0;
        for (Date date : shareDateSet) {
            RateAmountDiffRecord outerDiffRecord = outerMap.get(date);
            RateAmountDiffRecord innerDiffRecord = innerMap.get(date);
            if ((outerDiffRecord.amountDiff > outerAppAvgDiffNum && innerDiffRecord.amountDiff > innerAppAvgDiffNum))
                duplicateCount++;
        }


        if (duplicateCount >=  dataController.RATE_NUM_MIN_NUM) {
            if (rateNumGroupMap.containsKey(outerAppId)) {
                AppCluster appCluster = rateNumGroupMap.get(outerAppId);
                appCluster.getAppIdSet().add(innerAppId);
            } else {
                AppCluster newGroup = new AppCluster();
                newGroup.getAppIdSet().add(outerAppId);
                newGroup.getAppIdSet().add(innerAppId);
                rateNumGroupMap.put(outerAppId, newGroup);
            }
        }
    }

    public void mapRecursiveCombine(double rate) {

        boolean hasDuplicateSet = false;
        Object[] outerRankGroupArray = rateNumGroupMap.entrySet().toArray();
        Object[] innerRankGroupArray = rateNumGroupMap.entrySet().toArray();

        for (int i = 0; i < outerRankGroupArray.length; i++) {
            for (int j = i + 1; j < innerRankGroupArray.length; j++) {

                Map.Entry outerEntry = (Map.Entry) outerRankGroupArray[i];
                Map.Entry innerEntry = (Map.Entry) innerRankGroupArray[j];

                String outerId = outerEntry.getKey().toString();
                String innerId = innerEntry.getKey().toString();

                AppCluster outerAppCluster = (AppCluster) outerEntry.getValue();
                AppCluster innerAppCluster = (AppCluster) innerEntry.getValue();

                int outerGroupSize = outerAppCluster.getAppSize();
                int innerGroupSize = innerAppCluster.getAppSize();

                if (outerAppCluster.getAppIdSet().containsAll(innerAppCluster.getAppIdSet())
                        || innerAppCluster.getAppIdSet().containsAll(outerAppCluster.getAppIdSet())
                        || enableCombine(innerAppCluster.getAppIdSet(), outerAppCluster.getAppIdSet(), rate)) {

                    if (outerGroupSize > innerGroupSize)
                        rateNumGroupMap.remove(innerId);
                    else
                        rateNumGroupMap.remove(outerId);
                    hasDuplicateSet = true;
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
            mapRecursiveCombine(rate, rateNumGroupMap);
    }


    private boolean enableCombine(Set<String> setA, Set<String> setB, double rate) {
        Set<String> unionSet = Sets.union(setA, setB);
        Set<String> intersectionSet = Sets.intersection(setA, setB);

        double unionSize = unionSet.size();
        double intersectionSize = intersectionSet.size();

        return (intersectionSize / unionSize) >= rate;
    }

    public void generateExportDate() {
        Iterator iterator = rateNumGroupMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            AppCluster group = (AppCluster) entry.getValue();
            if (group.getAppSize() > 3) {
                Set set = group.getAppIdSet();
                Object[] array = set.toArray();
                String idA = array[0].toString();
                String idB = array[1].toString();
                String idC = array[2].toString();
                Map mapA = diffRecordMap.get(idA);
                Map mapB = diffRecordMap.get(idB);
                Map mapC = diffRecordMap.get(idC);
                Set setA = mapA.keySet();
                Set setB = mapB.keySet();
                Set setC = mapC.keySet();
                Set shareSet = Sets.intersection(setA, setB);
                shareSet = Sets.intersection(shareSet, setC);
                Iterator it = shareSet.iterator();
                while (it.hasNext()) {
                    Date date = (Date) it.next();
                    RateAmountDiffRecord recordA = (RateAmountDiffRecord) mapA.get(date);
                    RateAmountDiffRecord recordB = (RateAmountDiffRecord) mapB.get(date);
                    RateAmountDiffRecord recordC = (RateAmountDiffRecord) mapC.get(date);
                    AppData appA = (AppData) appMetaDataMap.get(idA);
                    AppData appB = (AppData) appMetaDataMap.get(idB);
                    AppData appC = (AppData) appMetaDataMap.get(idC);
                    dataController.insertTestDataToDb(date, recordA.amountDiff, recordB.amountDiff, recordC.
                            amountDiff, appA.averageDailyRateNum, appB.averageDailyRateNum, appC.averageDailyRateNum);

                }
            }
        }
    }

    private static class DateComparator implements Comparator<AppData> {
        public int compare(AppData app1, AppData app2) {
            Date date1 = app1.date;
            Date date2 = app2.date;
            if (date1.after(date2))
                return 1;
            else if (date2.after(date1))
                return -1;
            else
                return 0;
        }
    }
}

