package org.commcare.api.persistence;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.database.DatabaseIndexingUtils;
import org.commcare.modern.database.IndexedFixturePathsConstants;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A sandbox for user data using SqliteIndexedStorageUtility. Sandbox is per-User
 *
 * @author wspride
 */
public class UserSqlSandbox extends UserSandbox {
    private final SqliteIndexedStorageUtility<Case> caseStorage;
    private final SqliteIndexedStorageUtility<Ledger> ledgerStorage;
    private final SqliteIndexedStorageUtility<User> userStorage;
    private final SqliteIndexedStorageUtility<FormInstance> userFixtureStorage;
    private final SqliteIndexedStorageUtility<FormInstance> appFixtureStorage;
    private final SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> sqlUtil;
    private final String username, path;
    private User user = null;
    public static final String DEFAULT_DATBASE_PATH = "dbs";

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     */
    public UserSqlSandbox(String username, String path) {
        this.username = username;
        this.path = path;

        //we can't name this table "Case" becase that's reserved by sqlite
        caseStorage = new SqliteIndexedStorageUtility<>(Case.class, username, "CCCase", path);
        ledgerStorage = new SqliteIndexedStorageUtility<>(Ledger.class, username, Ledger.STORAGE_KEY, path);
        userStorage = new SqliteIndexedStorageUtility<>(User.class, username, User.STORAGE_KEY, path);
        userFixtureStorage = new SqliteIndexedStorageUtility<>(FormInstance.class, username, "UserFixture", path);
        appFixtureStorage = new SqliteIndexedStorageUtility<>(FormInstance.class, username, "AppFixture", path);
        sqlUtil = createFixturePathsTable(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE);
    }

    public UserSqlSandbox(String username) {
        this(username, DEFAULT_DATBASE_PATH);
    }

    @Override
    public SqliteIndexedStorageUtility<Case> getCaseStorage() {
        return caseStorage;
    }

    @Override
    public SqliteIndexedStorageUtility<Ledger> getLedgerStorage() {
        return ledgerStorage;
    }

    @Override
    public SqliteIndexedStorageUtility<User> getUserStorage() {
        return userStorage;
    }

    @Override
    public IStorageUtilityIndexed<StorageIndexedTreeElementModel> getIndexedFixtureStorage(String fixtureName) {
        String tableName = StorageIndexedTreeElementModel.getTableName(fixtureName);
        return new SqliteIndexedStorageUtility<>(StorageIndexedTreeElementModel.class, username, tableName, path);
    }

    @Override
    public void setupIndexedFixtureStorage(String fixtureName,
                                           StorageIndexedTreeElementModel exampleEntry,
                                           Set<String> indices) {
        String tableName = StorageIndexedTreeElementModel.getTableName(fixtureName);
        SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> sqlUtil =
                new SqliteIndexedStorageUtility<>(username, tableName, path);

        sqlUtil.rebuildTable(exampleEntry);

        sqlUtil.executeStatements(DatabaseIndexingUtils.getIndexStatements(tableName, indices));
    }

    @Override
    public Pair<String, String> getIndexedFixturePathBases(String fixtureName) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = sqlUtil.getConnection();
            preparedStatement =
                    connection.prepareStatement(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE_SELECT_STMT);
            preparedStatement.setString(1, fixtureName);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String base = resultSet.getString(1);
                String child = resultSet.getString(2);
                return new Pair<>(base, child);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void setIndexedFixturePathBases(String fixtureName, String baseName,
                                           String childName) {
        String tableName = StorageIndexedTreeElementModel.getTableName(fixtureName);
        SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> sqlUtil =
                createFixturePathsTable(tableName);
        Map<String, String> contentVals = new HashMap<>();
        contentVals.put(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_BASE, baseName);
        contentVals.put(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_CHILD, childName);
        contentVals.put(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_NAME, fixtureName);
        sqlUtil.basicInsert(contentVals);
    }

    /**
     * create 'fixture paths' table and an index over that table
     */
    private SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> createFixturePathsTable(String tableName) {
        // NOTE PLM: this should maybe be done on server startup instead on
        // ever invocation
        SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> sqlUtil =
                new SqliteIndexedStorageUtility<>(username, tableName, path);
        String[] indexTableStatements = new String[]{
                IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE_STMT
                // NOTE PLM: commenting out index creation below because
                // it will crash if run multiple times. We should find a way to
                // establish the index.
                // , IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEXING_STMT
        };
        sqlUtil.executeStatements(indexTableStatements);

        return sqlUtil;
    }

    @Override
    public SqliteIndexedStorageUtility<FormInstance> getUserFixtureStorage() {
        return userFixtureStorage;
    }

    @Override
    public SqliteIndexedStorageUtility<FormInstance> getAppFixtureStorage() {
        return appFixtureStorage;
    }

    @Override
    public User getLoggedInUser() {
        if (user == null) {
            SqliteIndexedStorageUtility<User> userStorage = getUserStorage();
            JdbcSqlStorageIterator<User> iterator = userStorage.iterate();
            if (iterator.hasMore()) {
                // should be only one user here
                user = iterator.next();
            } else {
                user = null;
            }
            iterator.closeConnection();
        }
        return user;
    }

    @Override
    public void setLoggedInUser(User user) {
        this.user = user;
    }

    //TODO WSP implement sync token stuff in next iteration, but useful to have in superclass now for AndroidSandbox
}
