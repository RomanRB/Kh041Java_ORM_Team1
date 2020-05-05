package simpleorm;

import annotations.Id;
import annotations.Table;
import com.sun.xml.internal.ws.api.model.wsdl.WSDLOutput;
import connectiontodb.ConnectionPoll;
import connectiontodb.DBConnection;
import crud_services.CRUDService;
import crud_services.SimpleORMInterface;
import relationannotation.ProcessOneToMany;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SimpleORM {

    private static List<String> existingTables = new ArrayList<>();

    /**
     * Save external
     *
     * @param object
     */

    public void save(Object object) {
// waiting for Marina's script
        if (!ifTableExists(object)) {
//            call to create table

        }
        saveObject(object);
        ProcessOneToMany.saveOneToMany(object);
        //ManyToOne...

    }


    /**
     * Update external
     *
     * @param object
     */

    public void update(Object object) {
// waiting for Marina's script
        if (!ifTableExists(object)) {
//            call to create table

        }
        updateObject(object);
        ProcessOneToMany.updateOneToMany(object);
        //ManyToOne...

    }

    public void delete(Object object){
        String tableName = object.getClass().getAnnotation(Table.class).name();
        Connection connection = ConnectionPoll.getConnection();
        int id = getObjectId(object);
        CRUDService crudService = new CRUDService(connection, object.getClass());
        crudService.deleteByIdCRUD(id);
    }


    private int getObjectId(Object object){
        int objectId = 0;
        try {
            Field[] fields = object.getClass().getDeclaredFields();
            for(Field f : fields){
                if (f.isAnnotationPresent(Id.class)){
                    f.setAccessible(true);
                    objectId = Integer.parseInt(f.get(object).toString());
                    f.setAccessible(false);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return objectId;
    }


    public Object selectByRowId(int id, Class clazz) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, clazz);
        Object object = crudService.selectById(id, clazz);
        ConnectionPoll.releaseConnection(connection);

        return object;
    }


    public List<Object> selectAllToObject(Class clazz) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, clazz);
        ConnectionPoll.releaseConnection(connection);
        return crudService.selectAll(clazz);
    }


    public List<String> selectAllToString(Class clazz) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, clazz);
        ConnectionPoll.releaseConnection(connection);
        return crudService.selectAllToString(clazz);
    }


    ///INTERNAL METHODS

    private boolean ifTableExists(Object object) {
        if (existingTables.size() > 0) {
            String tableName = object.getClass().getAnnotation(Table.class).name();
            System.out.println(tableName + " tablename that we got");
            for (String s : existingTables) {
                if (tableName.equals(s))
                    return true;
            }
        }
        return ifTableExistsRequestToTable(object);
    }


    private boolean ifTableExistsRequestToTable(Object object) {
        String tableName = object.getClass().getAnnotation(Table.class).name();
        Connection connection = ConnectionPoll.getConnection();
        ResultSet resultSet;

        StringBuilder sql = new StringBuilder("SELECT TABLE_NAME FROM information_schema.tables");
        sql.append(" WHERE table_schema = ? AND table_name = ? LIMIT 1;");

        try (PreparedStatement checkTable = connection.prepareStatement(sql.toString())) {
            checkTable.setString(1, DBConnection.getDBName());
            checkTable.setString(2, tableName);
            resultSet = checkTable.executeQuery();

            while (resultSet.next()) {
                return resultSet.getString("TABLE_NAME").equals(tableName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // update internal object
    private void updateObject(Object object) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, object.getClass());

        try {
            crudService.update((SimpleORMInterface) object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        ConnectionPoll.releaseConnection(connection);

        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    //internal method to save received object in save()
    private void saveObject(Object object) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, object.getClass());
        try {
            Field[] fields = object.getClass().getDeclaredFields();
            Field id = null;
            for(Field f : fields){
                if (f.isAnnotationPresent(Id.class)){
                    id = f;
                }
            }
            id.setAccessible(true);

            if (Integer.parseInt(id.get(object).toString()) != 0 && !id.get(object).equals(null)) {
                crudService.update((SimpleORMInterface) object);
            } else {
                crudService.insert((SimpleORMInterface) object);
            }
            id.setAccessible(false);
            ConnectionPoll.releaseConnection(connection);

        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


}


