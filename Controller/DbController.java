package Controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * Created by chenhao on 3/24/16.
 */
public class DbController {
    public static final String url = "jdbc:mysql://127.0.0.1/Data";
    public static final String name = "com.mysql.jdbc.Driver";
    public static final String user = "";
    public static final String password = "";
    public static final String rankQuerySql = "select appId,rankType,currentVersion,currentVersionReleaseDate,userRatingCountForCurrentVersion,userRatingCount,date from Data.AppInfo where rankType='update' and appId=? order by date";
    public static final String insertTestSql = "insert into Data.RateNumTest (date,appA,appB,appC,avgA,avgB,avgC) values (?,?,?,?,?,?,?)";
    public static final String insertAppGroupSql = "insert into Data.AppGroup (groupId,appId) values (?,?)";
    public static final String insertRankAppSql = "insert into Data.RankApp (rankMinNum,validAppAmount) values (?,?)";
    public static final String insertDistributionSql = "insert into Data.Distribution (Sim,Amount,Type) values (?,?,?)";
    public static final String insertAppPairSQL = "insert into Data.AppPair (appA,appB,support,label) values (?,?,?,?)";

    public Connection connection = null;
    public PreparedStatement rankNumQueryStmt = null;
    public PreparedStatement insertRateNumTestStmt = null;
    public PreparedStatement insertAppGroupStmt = null;
    public PreparedStatement insertAppRankStmt = null;
    public PreparedStatement insertDistributionStmt = null;
    public PreparedStatement insertAppPairStmt;

    public DbController() {
        try {
            Class.forName(name);
            connection = DriverManager.getConnection(url, user, password);
            //connection test
            System.out.println("Connect Database Success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        DbController dbController = new DbController();
    }

    public void setInsertAppPairStmt(String sql) {
        try {
            insertAppPairStmt = connection.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRankNumQueryStmt(String sql) {
        try {
            rankNumQueryStmt = connection.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setInsertRateNumTestStmt(String sql) {
        try {
            insertRateNumTestStmt = connection.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setInsertRankAppStmt(String sql) {
        try {
            insertAppRankStmt = connection.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setInsertAppGroupStmt(String sql) {
        try {
            insertAppGroupStmt = connection.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setInsertDistributionStmt(String sql) {
        try {
            insertDistributionStmt = connection.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
