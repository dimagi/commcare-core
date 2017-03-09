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
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private Connection connection;

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     */
    public UserSqlSandbox(String username, String path) {
        this.username = username;
        this.path = path;
        DataSource dataSource = getDataSource();
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //we can't name this table "Case" becase that's reserved by sqlite
        caseStorage = new SqliteIndexedStorageUtility<>(connection, Case.class, path, username, "CCCase");
        ledgerStorage = new SqliteIndexedStorageUtility<>(connection, Ledger.class, path, username, Ledger.STORAGE_KEY);
        userStorage = new SqliteIndexedStorageUtility<>(connection, User.class, path, username, User.STORAGE_KEY);
        userFixtureStorage = new SqliteIndexedStorageUtility<>(connection, FormInstance.class, path, username, "UserFixture");
        appFixtureStorage = new SqliteIndexedStorageUtility<>(connection, FormInstance.class, path, username, "AppFixture");
        sqlUtil = createFixturePathsTable(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE);
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = getDataSource(username, path).getConnection();
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            this.connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SQLiteConnectionPoolDataSource getDataSource() {
        return getDataSource(username, path);
    }

    public static SQLiteConnectionPoolDataSource getDataSource(String databaseName, String databasePath) {
        File databaseFolder = new File(databasePath);

        try {
            if (!databaseFolder.exists()) {
                Files.createDirectories(databaseFolder.toPath());
            }
            Class.forName("org.sqlite.JDBC");
            SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
            dataSource.setUrl("jdbc:sqlite:" + databasePath + "/" + databaseName + ".db");
            dataSource.getConnection().setAutoCommit(false);
            return dataSource;
        } catch (ClassNotFoundException|SQLException|IOException e) {
            throw new RuntimeException(e);
        }
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
        return new SqliteIndexedStorageUtility<>(connection,
                StorageIndexedTreeElementModel.class,
                path,
                username,
                tableName,
                false);
    }

    @Override
    public void setupIndexedFixtureStorage(String fixtureName,
                                           StorageIndexedTreeElementModel exampleEntry,
                                           Set<String> indices) {
        String tableName = StorageIndexedTreeElementModel.getTableName(fixtureName);
        SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> sqlUtil
                = new SqliteIndexedStorageUtility<>(connection, exampleEntry, path, username, tableName);
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
                new SqliteIndexedStorageUtility<>(connection, null, path, username, tableName, false);
        String[] indexTableStatements = new String[]{
                IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE_STMT,
                // NOTE PLM: commenting out index creation below because
                // it will crash if run multiple times. We should find a way to
                // establish the index.
                IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEXING_STMT
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
            //iterator.closeConnection();
        }
        return user;
    }

    @Override
    public void setLoggedInUser(User user) {
        this.user = user;
    }

    public void closeConnection() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
