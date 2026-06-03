package org.search.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseContext {
    private static final String DB_URL = "jdbc:h2:./search_db";
    private static final String USER = "sa";
    private static final String PASS = "";

    private static volatile Connection sharedConnection;

    private DatabaseContext() {
    }

    public static Connection getJdbcConnection() throws SQLException {
        Connection shared = sharedConnection;
        if (shared == null || shared.isClosed()) {
            synchronized (DatabaseContext.class) {
                shared = sharedConnection;
                if (shared == null || shared.isClosed()) {
                    shared = DriverManager.getConnection(DB_URL, USER, PASS);
                    sharedConnection = shared;
                }
            }
        }
        return shared;
    }

    public static Connection getConnection() throws SQLException {
        return wrapNonClosing(getJdbcConnection());
    }

    private static Connection wrapNonClosing(Connection delegate) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("close".equals(method.getName())) {
                return null;
            }
            try {
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof SQLException sql) {
                    throw sql;
                }
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new SQLException(cause);
            }
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler);
    }
}
