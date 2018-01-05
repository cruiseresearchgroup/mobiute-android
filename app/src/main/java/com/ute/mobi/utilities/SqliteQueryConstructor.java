package com.ute.mobi.utilities;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public class SqliteQueryConstructor {
  public static class SqliteColumnDesc {
    public String columnName;
    public String dataType;
    public String defaultDef;
    public boolean isNullable;

    public SqliteColumnDesc(String columnName, String dataType, String defaultDef, boolean isNullable) {
      this.columnName = columnName;
      this.dataType = dataType;
      this.defaultDef = defaultDef;
      this.isNullable = isNullable;
    }
  }

  public static String CREATETABLE_IFNOTEXIST(String tablename, SqliteColumnDesc... args) {
    if(args == null && args.length <= 0)
      return null;

    String query = "CREATE TABLE IF NOT EXISTS " + tablename;
    query += "(";
    for(int i = 0; i < args.length; i++) {
      SqliteColumnDesc desc = args[i];
      //id integer primary key autoincrement
      if(i != 0) {
        query += ", ";
      }
      query += desc.columnName + " " + desc.dataType + " " + desc.defaultDef;
      if(desc.isNullable == false) {
        query += " " + "NOT NULL";
      }
    }

    query += ");";
    return query;
  }
}
