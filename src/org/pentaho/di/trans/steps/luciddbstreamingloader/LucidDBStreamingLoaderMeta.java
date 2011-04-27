/*
 * Copyright (c) 2007 Pentaho Corporation.  All rights reserved. 
 * Copyright (c) 2010 DynamoBI Corporation.  All rights reserved.
 * This software was developed by DynamoBI Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is LucidDB 
 * Streaming Loader.  The Initial Developer is DynamoBI Corporation.
 * 
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.
 */

package org.pentaho.di.trans.steps.luciddbstreamingloader;

import java.util.List;
import java.util.Map;

import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.SQLStatement;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.shared.SharedObjectInterface;
import org.pentaho.di.trans.DatabaseImpact;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

/**
 * Description: Hold data for LucidDB Streaming loader dialog/UI
 * 
 * @author ngoodman
 * 
 */
public class LucidDBStreamingLoaderMeta extends BaseStepMeta implements
    StepMetaInterface {
  private static Class<?> PKG = LucidDBStreamingLoaderMeta.class; // for i18n
  
  public static String TARGET_TABLE_ALIAS = "TGT";
  public static String SOURCE_TABLE_ALIAS = "SRC";
  
  public static String REMOTE_ROWS_UDX = "APPLIB.REMOTE_ROWS";
  
  public static String OPERATION_MERGE = "MERGE";
  public static String OPERATION_INSERT = "INSERT";
  public static String OPERATION_UPDATE = "UPDATE";
  public static String OPERATION_CUSTOM = "CUSTOM";
  

  

  // purposes,
  // needed by
  // Translator2!!
  // $NON-NLS-1$

  /** what's the schema for the target? */
  private String schemaName;

  /** what's the table for the target? */
  private String tableName;

  /** database connection */
  private DatabaseMeta databaseMeta;

  /** Host URL */
  private String host;

  /** Host port */
  private String port;

  /** DB Operation */
  private String operation;

  /** Field name of the target table in tabitem Keys */
  private String fieldTableForKeys[];

  /** Field name in the stream in tabitem Keys */
  private String fieldStreamForKeys[];

  /** Field name of the target table in tabitem Fields */
  private String fieldTableForFields[];

  /** Field name in the stream in tabitem Fields */
  private String fieldStreamForFields[];

  /** flag to indicate Insert or Update operation for LucidDB in tabitem Fields */
  private boolean insOrUptFlag[];

  /** It holds custom sql statements in CUSTOM Tab */
  private String custom_sql;

  /** It keep whether all components in tab is enable or not */
  private boolean tabIsEnable[];

  public boolean[] getTabIsEnable() {
    return tabIsEnable;
  }

  public void setTabIsEnable(boolean[] tabIsEnable) {
    this.tabIsEnable = tabIsEnable;
  }


  public LucidDBStreamingLoaderMeta() {
    super();
  }

  /**
   * @return Returns the database.
   */
  public DatabaseMeta getDatabaseMeta() {
    return databaseMeta;
  }

  /**
   * @param database
   *          The database to set.
   */
  public void setDatabaseMeta(DatabaseMeta database) {
    this.databaseMeta = database;
  }

  /**
   * @return Returns the tableName.
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * @param tableName
   *          The tableName to set.
   */
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public void loadXML(Node stepnode, List<DatabaseMeta> databases,
      Map<String, Counter> counters) throws KettleXMLException {
    readData(stepnode, databases);
  }

  public void allocate(int nrKeyMapping, int nrFieldMapping, int nrTabIsEnable) {
    // for Keys Tab
    fieldTableForKeys = new String[nrKeyMapping];
    fieldStreamForKeys = new String[nrKeyMapping];
    // for Fields Tab
    fieldTableForFields = new String[nrFieldMapping];
    fieldStreamForFields = new String[nrFieldMapping];
    insOrUptFlag = new boolean[nrFieldMapping];

    tabIsEnable = new boolean[nrTabIsEnable];
  }

  public Object clone() {
    LucidDBStreamingLoaderMeta retval = (LucidDBStreamingLoaderMeta) super
        .clone();
    int nrKeyMapping = fieldTableForKeys.length;
    int nrFieldMapping = fieldTableForFields.length;
    int nrTabIsEnable = tabIsEnable.length;
    retval.allocate(nrKeyMapping, nrFieldMapping, nrTabIsEnable);

    for (int i = 0; i < nrKeyMapping; i++) {
      retval.fieldTableForKeys[i] = fieldTableForKeys[i];
      retval.fieldStreamForKeys[i] = fieldStreamForKeys[i];
    }

    for (int i = 0; i < nrFieldMapping; i++) {
      retval.fieldTableForFields[i] = fieldTableForFields[i];
      retval.fieldStreamForFields[i] = fieldStreamForFields[i];
      retval.insOrUptFlag[i] = insOrUptFlag[i];
    }

    for (int i = 0; i < nrTabIsEnable; i++) {
      retval.tabIsEnable[i] = tabIsEnable[i];

    }

    return retval;
  }

  private void readData(Node stepnode,
      List<? extends SharedObjectInterface> databases)
      throws KettleXMLException {
    try {
      String con = XMLHandler.getTagValue(stepnode, "connection"); //$NON-NLS-1$
      databaseMeta = DatabaseMeta.findDatabase(databases, con);
      schemaName = XMLHandler.getTagValue(stepnode, "schema"); //$NON-NLS-1$
      tableName = XMLHandler.getTagValue(stepnode, "table"); //$NON-NLS-1$
      host = XMLHandler.getTagValue(stepnode, "host"); //$NON-NLS-1$
      port = XMLHandler.getTagValue(stepnode, "port"); //$NON-NLS-1$
      operation = XMLHandler.getTagValue(stepnode, "operation"); //$NON-NLS-1$
      custom_sql = XMLHandler.getTagValue(stepnode, "custom_sql"); //$NON-NLS-1$
      int nrKeyMapping = XMLHandler.countNodes(stepnode, "keys_mapping"); //$NON-NLS-1$
      int nrFieldMapping = XMLHandler.countNodes(stepnode, "fields_mapping"); //$NON-NLS-1$
      int nrTabIsEnable = XMLHandler.countNodes(stepnode,
          "tab_is_enable_mapping"); //$NON-NLS-1$
      allocate(nrKeyMapping, nrFieldMapping, nrTabIsEnable);

      for (int i = 0; i < nrKeyMapping; i++) {
        Node vnode = XMLHandler.getSubNodeByNr(stepnode, "keys_mapping", i); //$NON-NLS-1$

        fieldTableForKeys[i] = XMLHandler.getTagValue(vnode, "key_field_name"); //$NON-NLS-1$
        fieldStreamForKeys[i] = XMLHandler
            .getTagValue(vnode, "key_stream_name"); //$NON-NLS-1$
        if (fieldStreamForKeys[i] == null)
          fieldStreamForKeys[i] = fieldTableForKeys[i]; // default:
        // the same
        // name!

      }
      for (int i = 0; i < nrFieldMapping; i++) {
        Node vnode = XMLHandler.getSubNodeByNr(stepnode, "fields_mapping", i); //$NON-NLS-1$

        fieldTableForFields[i] = XMLHandler.getTagValue(vnode,
            "field_field_name"); //$NON-NLS-1$
        fieldStreamForFields[i] = XMLHandler.getTagValue(vnode,
            "field_stream_name"); //$NON-NLS-1$
        if (fieldStreamForFields[i] == null)
          fieldStreamForFields[i] = fieldTableForFields[i]; // default:
        // the
        // same
        // name!
        insOrUptFlag[i] = "Y".equalsIgnoreCase(XMLHandler.getTagValue(vnode, "insert_or_update_flag")); //$NON-NLS-1$

      }

      for (int i = 0; i < nrTabIsEnable; i++) {
        Node vnode = XMLHandler.getSubNodeByNr(stepnode,
            "tab_is_enable_mapping", i); //$NON-NLS-1$
        tabIsEnable[i] = "Y".equalsIgnoreCase(XMLHandler.getTagValue(vnode, "tab_is_enable")); //$NON-NLS-1$

      }

    } catch (Exception e) {
      throw new KettleXMLException(
          BaseMessages
              .getString(PKG,
                  "LucidDBStreamingLoaderMeta.Exception.UnableToReadStepInfoFromXML"), e); //$NON-NLS-1$
    }
  }

  public void setDefault() {
    databaseMeta = null;
    schemaName = ""; //$NON-NLS-1$
    tableName = BaseMessages.getString(PKG,
        "LucidDBStreamingLoaderMeta.DefaultTableName"); //$NON-NLS-1$      
    host = "localhost";
    port = "9034";
    operation = "MERGE";
    allocate(0, 0, 0);
  }

  public String getXML() {
    StringBuffer retval = new StringBuffer(300);

    retval
        .append("    ").append(XMLHandler.addTagValue("connection", databaseMeta == null ? "" : databaseMeta.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    retval.append("    ").append(XMLHandler.addTagValue("schema", schemaName)); //$NON-NLS-1$ //$NON-NLS-2$
    retval.append("    ").append(XMLHandler.addTagValue("table", tableName)); //$NON-NLS-1$ //$NON-NLS-2$
    retval.append("    ").append(XMLHandler.addTagValue("host", host)); //$NON-NLS-1$ //$NON-NLS-2$
    retval.append("    ").append(XMLHandler.addTagValue("port", port)); //$NON-NLS-1$ //$NON-NLS-2$
    retval
        .append("    ").append(XMLHandler.addTagValue("operation", operation)); //$NON-NLS-1$ //$NON-NLS-2$
    retval
        .append("    ").append(XMLHandler.addTagValue("custom_sql", custom_sql)); //$NON-NLS-1$ //$NON-NLS-2$

    for (int i = 0; i < fieldTableForKeys.length; i++) {
      retval.append("      <keys_mapping>").append(Const.CR); //$NON-NLS-1$
      retval
          .append("        ").append(XMLHandler.addTagValue("key_field_name", fieldTableForKeys[i])); //$NON-NLS-1$ //$NON-NLS-2$
      retval
          .append("        ").append(XMLHandler.addTagValue("key_stream_name", fieldStreamForKeys[i])); //$NON-NLS-1$ //$NON-NLS-2$
      retval.append("      </keys_mapping>").append(Const.CR); //$NON-NLS-1$
    }

    for (int i = 0; i < fieldTableForFields.length; i++) {
      retval.append("      <fields_mapping>").append(Const.CR); //$NON-NLS-1$
      retval
          .append("        ").append(XMLHandler.addTagValue("field_field_name", fieldTableForFields[i])); //$NON-NLS-1$ //$NON-NLS-2$
      retval
          .append("        ").append(XMLHandler.addTagValue("field_stream_name", fieldStreamForFields[i])); //$NON-NLS-1$ //$NON-NLS-2$
      retval
          .append("        ").append(XMLHandler.addTagValue("insert_or_update_flag", insOrUptFlag[i])); //$NON-NLS-1$ //$NON-NLS-2$
      retval.append("      </fields_mapping>").append(Const.CR); //$NON-NLS-1$
    }

    for (int i = 0; i < tabIsEnable.length; i++) {
      retval.append("      <tab_is_enable_mapping>").append(Const.CR); //$NON-NLS-1$
      retval
          .append("        ").append(XMLHandler.addTagValue("tab_is_enable", tabIsEnable[i])); //$NON-NLS-1$ //$NON-NLS-2$
      retval.append("      </tab_is_enable_mapping>").append(Const.CR); //$NON-NLS-1$
    }

    return retval.toString();
  }

  public void readRep(Repository rep, ObjectId id_step,
      List<DatabaseMeta> databases, Map<String, Counter> counters)
      throws KettleException {
    try {
      databaseMeta = rep.loadDatabaseMetaFromStepAttribute(id_step,
          "id_connection", databases);
      schemaName = rep.getStepAttributeString(id_step, "schema"); //$NON-NLS-1$
      tableName = rep.getStepAttributeString(id_step, "table"); //$NON-NLS-1$
      host = rep.getStepAttributeString(id_step, "host"); //$NON-NLS-1$
      port = rep.getStepAttributeString(id_step, "port"); //$NON-NLS-1$
      operation = rep.getStepAttributeString(id_step, "operation"); //$NON-NLS-1$
      custom_sql = rep.getStepAttributeString(id_step, "custom_sql"); //$NON-NLS-1$
      int nrKeyMapping = rep.countNrStepAttributes(id_step, "key_field_name"); //$NON-NLS-1$
      int nrFieldMapping = rep.countNrStepAttributes(id_step, "field_field_name"); //$NON-NLS-1$
      int nrTabIsEnable = rep.countNrStepAttributes(id_step,
          "tab_is_enable_mapping"); //$NON-NLS-1$

      allocate(nrKeyMapping, nrFieldMapping, nrTabIsEnable);

      for (int i = 0; i < nrKeyMapping; i++) {
        fieldTableForKeys[i] = rep.getStepAttributeString(id_step, i,
            "key_field_name"); //$NON-NLS-1$
        fieldStreamForKeys[i] = rep.getStepAttributeString(id_step, i,
            "key_stream_name"); //$NON-NLS-1$
        if (fieldStreamForKeys[i] == null)
          fieldStreamForKeys[i] = fieldTableForKeys[i];

      }

      for (int i = 0; i < nrFieldMapping; i++) {
        fieldTableForFields[i] = rep.getStepAttributeString(id_step, i,
            "field_field_name"); //$NON-NLS-1$
        fieldStreamForFields[i] = rep.getStepAttributeString(id_step, i,
            "field_stream_name"); //$NON-NLS-1$
        if (fieldStreamForFields[i] == null)
          fieldStreamForFields[i] = fieldTableForFields[i];
        insOrUptFlag[i] = rep.getStepAttributeBoolean(id_step, i,
            "insert_or_update_flag"); //$NON-NLS-1$
      }

      for (int i = 0; i < nrTabIsEnable; i++) {

        tabIsEnable[i] = rep.getStepAttributeBoolean(id_step, i,
            "tab_is_enable"); //$NON-NLS-1$
      }
    } catch (Exception e) {
      throw new KettleException(
          BaseMessages
              .getString(
                  PKG,
                  "LucidDBStreamingLoaderMeta.Exception.UnexpectedErrorReadingStepInfoFromRepository"), e); //$NON-NLS-1$
    }
  }

  public void saveRep(Repository rep, ObjectId id_transformation,
      ObjectId id_step) throws KettleException {
    try {
      rep.saveDatabaseMetaStepAttribute(id_transformation, id_step,
          "id_connection", databaseMeta);
      rep.saveStepAttribute(id_transformation, id_step, "schema", schemaName); //$NON-NLS-1$
      rep.saveStepAttribute(id_transformation, id_step, "table", tableName); //$NON-NLS-1$  
      rep.saveStepAttribute(id_transformation, id_step, "host", host); //$NON-NLS-1
      rep.saveStepAttribute(id_transformation, id_step, "port", port); //$NON-NLS-1
      rep.saveStepAttribute(id_transformation, id_step, "operation", operation); //$NON-NLS-1$
      rep.saveStepAttribute(id_transformation, id_step,
          "custom_sql", custom_sql); //$NON-NLS-1$       

      for (int i = 0; i < fieldTableForKeys.length; i++) {
        rep.saveStepAttribute(id_transformation, id_step, i,
            "key_field_name", fieldTableForKeys[i]); //$NON-NLS-1$
        rep.saveStepAttribute(id_transformation, id_step, i,
            "key_stream_name", fieldStreamForKeys[i]); //$NON-NLS-1$

      }

      for (int i = 0; i < fieldTableForFields.length; i++) {
        rep.saveStepAttribute(id_transformation, id_step, i,
            "field_field_name", fieldTableForFields[i]); //$NON-NLS-1$
        rep.saveStepAttribute(id_transformation, id_step, i,
            "field_stream_name", fieldStreamForFields[i]); //$NON-NLS-1$
        rep.saveStepAttribute(id_transformation, id_step, i,
            "insert_or_update_flag", insOrUptFlag[i]); //$NON-NLS-1$
      }

      for (int i = 0; i < tabIsEnable.length; i++) {

        rep.saveStepAttribute(id_transformation, id_step, i,
            "tab_is_enable", tabIsEnable[i]); //$NON-NLS-1$
      }

      // Also, save the step-database relationship!
      if (databaseMeta != null)
        rep.insertStepDatabase(id_transformation, id_step, databaseMeta
            .getObjectId());
    } catch (Exception e) {
      throw new KettleException(
          BaseMessages
              .getString(PKG,
                  "LucidDBStreamingLoaderMeta.Exception.UnableToSaveStepInfoToRepository") + id_step, e); //$NON-NLS-1$
    }
  }

  public void getFields(RowMetaInterface rowMeta, String origin,
      RowMetaInterface[] info, StepMeta nextStep, VariableSpace space)
      throws KettleStepException {
    // Default: nothing changes to rowMeta
  }

  // TODO: In future, we need to implement it to do double-check.
  public void check(List<CheckResultInterface> remarks, TransMeta transMeta,
      StepMeta stepMeta, RowMetaInterface prev, String input[],
      String output[], RowMetaInterface info) {

  }
  
  /**
   * 
   */
  
  public boolean isInKeys(String streamFieldName) {
      
      for (int i = 0 ; i < fieldStreamForKeys.length ; i ++ ){
          if ( streamFieldName.equals(fieldStreamForKeys[i]))
              return true;
      }
      return false;
      
  }
  
  
  private String buildFakeCursorRowString( ValueMetaInterface v, String columnName ) throws KettleStepException{
   
         StringBuffer sb = new StringBuffer();
         sb.append("CAST (null AS ");
         sb.append(databaseMeta.getFieldDefinition(v,null, null, false, false, false));
         sb.append(") AS " + databaseMeta.getStartQuote() + columnName + databaseMeta.getEndQuote());
        
         return sb.toString();
  }
  
  private String buildRemoteRowsCursorFromInput(RowMetaInterface prev) throws KettleStepException {
	  
	  boolean suppress_comma = true;
	  
	  StringBuffer sb = new StringBuffer(300);
	  // Iterate over fieldStreamForKeys[]
	  
	  for ( int i = 0 ; i < fieldStreamForKeys.length ; i++ ) {
		
		  // Add comma to all except the first row
		if ( suppress_comma == true )
			suppress_comma = false;
		else
			sb.append(",");
		
		String keyStreamFieldName = fieldStreamForKeys[i];
		ValueMetaInterface keyStreamField = prev.searchValueMeta(fieldStreamForKeys[i]);
		
		if (keyStreamField==null) {
		  throw new KettleStepException("Unable to find key field '"+keyStreamFieldName+"' in the input fields");
		}
		
		sb.append(buildFakeCursorRowString(keyStreamField, keyStreamFieldName)).append(Const.CR);
		
	  }
	  
	  // Iterate over fieldStreamForFields[] (dedup)
	  for ( int i = 0 ; i < fieldStreamForFields.length ; i++ ) {
		  // Do not add if it's already in from keys
		if ( !isInKeys(fieldStreamForFields[i])) {		
			  // Add comma to all except the first row
			if ( suppress_comma == true )
				suppress_comma = false;
			else
				sb.append(",");
			
			sb.append(buildFakeCursorRowString(prev.searchValueMeta(fieldStreamForFields[i]), fieldStreamForFields[i]) + Const.CR);
		}
	  }
	  
	  return sb.toString();
	  
	  
  }
  
  private String buildRemoteRowsFragment (RowMetaInterface prev) throws KettleStepException
  {
      
      return buildRemoteRowsFragment(prev, false);
  }
  /*
   * Reviews the current keys, fields, and builds a select statement 
   * suitable for remoting the rows
   * ie, select * from table(remote_rows_udx( ..... ) as "SRC"
   * 
   */
  private String buildRemoteRowsFragment (RowMetaInterface prev, boolean statement_alone) throws KettleStepException {
      
      StringBuffer fragment = new StringBuffer();
      
      if ( !statement_alone) fragment.append("(");
      fragment.append("SELECT * FROM TABLE ( " + REMOTE_ROWS_UDX + "(" + Const.CR);
      // Param 1 is CURSOR format
      fragment.append("CURSOR (SELECT ");
      fragment.append(buildRemoteRowsCursorFromInput(prev));
      fragment.append(" FROM (VALUES(0)))" + Const.CR);
      // Param 2 is PORT
      fragment.append(" , " + getPort() + Const.CR);
      // Param 3 is IS_COMPRESSED
      fragment.append(" , false" + Const.CR);
      fragment.append(" ))");
      if ( !statement_alone) fragment.append(")");
      if ( !statement_alone) fragment.append(" AS " + databaseMeta.getStartQuote() + SOURCE_TABLE_ALIAS + databaseMeta.getEndQuote());
      
      
      if ( isDebug() )
      	logDebug("------buildRemoteRowsFragment------ " + fragment + "-----END buildRemoteRowsFragment------");
      
      return fragment.toString();
  }
  
  /**
   * Builds the target column list for use in the INSERT statement
   * INSERT INTO T1 <<columns>>
   * ie, ("Col1", "Col2")
   * @return
   */
  private String buildTargetColumnsForInsert() {
	  
	  boolean suppress_comma = true;
	  
	  StringBuffer sb = new StringBuffer(300);
	  sb.append("(");
	  // Iterate over fieldTableForKeys[]
	  
	  for ( int i = 0 ; i < fieldTableForKeys.length ; i++ ) {
		
		  // Add comma to all except the first row
		if ( suppress_comma == true )
			suppress_comma = false;
		else
			sb.append(",");
		
		sb.append(databaseMeta.getStartQuote() + fieldTableForKeys[i] + databaseMeta.getEndQuote());
		
	  }
	  
	  // Iterate over fieldTableForFields[] (dedup)
	  for ( int i = 0 ; i < fieldTableForFields.length ; i++ ) {
		  // Do not add if it's already in from keys
		if ( Const.indexOfString(fieldTableForFields[i], fieldTableForKeys)<0) {		
			  // Add comma to all except the first row
			if ( suppress_comma == true )
				suppress_comma = false;
			else
				sb.append(",");
			
			sb.append(databaseMeta.getStartQuote() + fieldTableForFields[i] + databaseMeta.getEndQuote());
		}
	  }
	  sb.append(")");
	  
	  return sb.toString();
	  
	  
  }
  
  /**
   * Builds the source column list for use in the MERGE statement
   * WHEN NOT MATCHED THEN
   * INSERT INTO T1 (tgt1, tg2)
   * VALUES <<columnlist>>
   * ie, ("SRC"."Field1", "SRC"."Field2")
   * @return
   */
  private String buildSourceColumnsForInsert() {
	  
	  boolean suppress_comma = true;
	  
	  StringBuffer sb = new StringBuffer(300);
	  sb.append("(");
	  // Iterate over fieldStreamForKeys[]
	  
	  for ( int i = 0 ; i < fieldStreamForKeys.length ; i++ ) {
		
		  // Add comma to all except the first row
		if ( suppress_comma == true )
			suppress_comma = false;
		else
			sb.append(",");
		
		sb.append(databaseMeta.getStartQuote() + fieldStreamForKeys[i] + databaseMeta.getEndQuote());
		
	  }
	  
	  // Iterate over fieldStreamForFields[] (dedup)
	  for ( int i = 0 ; i < fieldStreamForFields.length ; i++ ) {
		  // Do not add if it's already in from keys
		if ( !isInKeys(fieldStreamForFields[i])) {		
			  // Add comma to all except the first row
			if ( suppress_comma == true )
				suppress_comma = false;
			else
				sb.append(",");
			
			sb.append(databaseMeta.getStartQuote() + fieldStreamForFields[i] + databaseMeta.getEndQuote());
		}
	  }
	  sb.append(")");
	  
	  return sb.toString();
	  
	  
  }
  
  /**
   * Builds the match condition for MERGE stmt
   * MERGE INTO T1 USING SRC 
   * ON <<matchCondition>>
   * ie, "SRC"."Field1" = "TGT"."Table1" AND "SRC"."Field2" = "SRC"."Table2"
   * @return
   */
  
  private String buildMatchCondition () {
	  
	  StringBuffer matchCondition = new StringBuffer(300);

	  if (fieldStreamForKeys != null) {

	    for (int i = 0; i < fieldStreamForKeys.length; i++) {
	    	
	      // Only add AND for all subsequent conditions, but not the first
	      if ( i > 0 ) {
	    	  matchCondition.append(Const.CR + "AND ");
	      }

	      // "SRC"."FieldStreamName"
	      matchCondition.append(databaseMeta.getStartQuote() + SOURCE_TABLE_ALIAS + databaseMeta.getEndQuote());
	      matchCondition.append(".");
	      matchCondition.append(databaseMeta.getStartQuote() + fieldStreamForKeys[i] + databaseMeta.getEndQuote());
	      matchCondition.append(" = ");
	      // "TGT"."TableName"
	      matchCondition.append(databaseMeta.getStartQuote() + TARGET_TABLE_ALIAS + databaseMeta.getEndQuote());
	      matchCondition.append(".");
	      matchCondition.append(databaseMeta.getStartQuote() + fieldTableForKeys[i] + databaseMeta.getEndQuote());
	      matchCondition.append(Const.CR);
	    }

	  }
	  
	  return matchCondition.toString();

	  
  }
  
  /**
   * Builds the set statement for MERGE stmt
   * MERGE INTO T1 USING SRC 
   * ON CONDITION
   * WHEN MATCHED THEN UPDATE SET 
   * << setstatement >>
   * ie, "Col1" = "SRC"."Field1", "Col2" = "SRC"."Field2"
   */
  
  private String buildMergeSetString () {
  
	  boolean suppress_comma = true;
	  
	  StringBuffer sb = new StringBuffer();
	  
  // Iterate over fieldStreamForFields[] 
  for ( int i = 0 ; i < fieldStreamForFields.length ; i++ ) {
	
	  // Only added to this clause if Update Y/N is true
	  if ( insOrUptFlag[i] ) {
			  // Add comma to all except the first row
			if ( suppress_comma == true )
				suppress_comma = false;
			else
				sb.append(",");
			
			sb.append(databaseMeta.getStartQuote() + fieldTableForFields[i] + databaseMeta.getEndQuote());
			sb.append(" = ");
			sb.append(databaseMeta.getStartQuote() + SOURCE_TABLE_ALIAS + databaseMeta.getEndQuote() + ".");
			sb.append(databaseMeta.getStartQuote() + fieldStreamForFields[i] + databaseMeta.getEndQuote());
	  	}
  }
  
  	return sb.toString();
  
  
  }
    
  private String buildTargetTableString (){
	  
	  StringBuffer targetTable = new StringBuffer();
	  
	  targetTable.append(databaseMeta.getStartQuote() + getSchemaName() + databaseMeta.getEndQuote());
	  targetTable.append(".");
	  targetTable.append(databaseMeta.getStartQuote() + getTableName() + databaseMeta.getEndQuote());
	  
	  return targetTable.toString();
	  
	  
  }

  /**
   * Create DML Sql Statements for remote_rows
   * 
   * @param prev
   * @return
   * @throws KettleStepException
   */
  public String getDMLStatement(RowMetaInterface prev)
      throws KettleStepException {
    
   
	 if ( operation.equals(OPERATION_INSERT) ) {
    	
    	
    	StringBuffer insert = new StringBuffer();
    	
    	insert.append("INSERT INTO " + Const.CR);
    	insert.append(buildTargetTableString() + Const.CR);
    	insert.append(buildTargetColumnsForInsert() + Const.CR);
    	// Build statement ALONE! (no "as SRC"
    	insert.append(buildRemoteRowsFragment(prev, true));
    	
    	if (isDebug())
    		logDebug("-----INSERT----" + insert + "-----END INSERT-----");
    	
    	return insert.toString();
    }
    
    if ( operation.equals (OPERATION_MERGE) || operation.equals (OPERATION_UPDATE)  ) {
    	
    	StringBuffer merge = new StringBuffer();
    	
    	merge.append("MERGE INTO " + buildTargetTableString());
    	merge.append(" as " + databaseMeta.getStartQuote() + TARGET_TABLE_ALIAS + databaseMeta.getEndQuote() + Const.CR);
    	merge.append("USING " + buildRemoteRowsFragment(prev) + Const.CR);
    	merge.append("ON " + buildMatchCondition() + Const.CR);
    	merge.append("WHEN MATCHED THEN UPDATE SET " + Const.CR);
    	merge.append(buildMergeSetString() + Const.CR);
    	
    	// If UPDATE we're done (no INSERT clause)
    	// If we're actuall MERGEing we need to build WHEN NOT MATCHED THEN section
    	if ( operation.equals(OPERATION_MERGE) ) {
    		merge.append ("WHEN NOT MATCHED THEN " + Const.CR);
    		merge.append ("INSERT " + buildTargetColumnsForInsert() + Const.CR);
    		merge.append ("VALUES " + buildSourceColumnsForInsert() + Const.CR);
    		
    	}
    	
    	if (isDebug())
    		logDebug("-----MERGE or UPDATE----" + merge + "-----END MERGE or UPDATE-----");
    	
    	return merge.toString(); 
    }
    
    if ( operation.equals(OPERATION_CUSTOM) ) {
    	String custom = getCustom_sql().replace("?", buildRemoteRowsFragment(prev, true)); 
    	
    	if (isDebug())
    		logDebug("-----CUSTOM----" + custom + "-----END CUSTOM-----");
    	
    	return custom.toString();
    	
    }
	return "ERRORSQLSTATEMENT";
	
  }


  public String getCreateTableAsStatement ( RowMetaInterface prev ) throws KettleStepException {
	  
	  StringBuffer sb  = new StringBuffer() ;
	  
	  sb.append("CALL APPLIB.CREATE_TABLE_AS (" + Const.CR);
	  // Schema Name
	  sb.append("'" + getSchemaName() + "'" + Const.CR);
	  // Table Name
	  sb.append(",'" + getTableName() + "'" + Const.CR);
	  // select statement
	  sb.append(",'" + buildRemoteRowsFragment(prev) + "'" + Const.CR);
	  // should load false
	  sb.append(", false )");
	  

	  return sb.toString();
	  
	  
	  
	  
  }
  


//  public String getSQLDataType(ValueMetaInterface field) throws KettleStepException {
//
//    String dataType = "";
//
//    int length = field.getLength();
//
//    switch (field.getType()) {
//      case ValueMetaInterface.TYPE_NUMBER:
//        dataType = "DECIMAL(" + Integer.toString(length) + ", " +
//          Integer.toString(field.getPrecision()) + ")";
//        break;
//      case ValueMetaInterface.TYPE_STRING:
//        dataType = "VARCHAR(" + Integer.toString(length) + ")";
//        break;
//      case ValueMetaInterface.TYPE_DATE:
//        dataType = "DATE";
//        break;
//      case ValueMetaInterface.TYPE_BOOLEAN:
//        dataType = "BOOLEAN";
//        break;
//      case ValueMetaInterface.TYPE_INTEGER:
//        dataType = "INT";
//        break;
//      case ValueMetaInterface.TYPE_BIGNUMBER:
//        dataType = "BIGINT";
//        break;
//      case ValueMetaInterface.TYPE_BINARY:
//        dataType = "BINARY";
//        break;
//    }
//    return dataType;
//
//  }

  // TODO: Not know the purpose of this method yet so far.
  public void analyseImpact(List<DatabaseImpact> impact, TransMeta transMeta,
      StepMeta stepMeta, RowMetaInterface prev, String input[],
      String output[], RowMetaInterface info) throws KettleStepException {

  }

  public StepInterface getStep(StepMeta stepMeta,
      StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
      Trans trans) {
    return new LucidDBStreamingLoader(stepMeta, stepDataInterface, cnr,
        transMeta, trans);
  }

  public StepDataInterface getStepData() {
    return new LucidDBStreamingLoaderData();
  }

  public DatabaseMeta[] getUsedDatabaseConnections() {
    if (databaseMeta != null) {
      return new DatabaseMeta[] { databaseMeta };
    } else {
      return super.getUsedDatabaseConnections();
    }
  }

  public RowMetaInterface getRequiredFields(VariableSpace space)
      throws KettleException {
    String realTableName = space.environmentSubstitute(tableName);
    String realSchemaName = space.environmentSubstitute(schemaName);

    if (databaseMeta != null) {
      Database db = new Database(loggingObject, databaseMeta);
      try {
        db.connect();

        if (!Const.isEmpty(realTableName)) {
          String schemaTable = databaseMeta.getQuotedSchemaTableCombination(
              realSchemaName, realTableName);

          // Check if this table exists...
          if (db.checkTableExists(schemaTable)) {
            return db.getTableFields(schemaTable);
          } else {
            throw new KettleException(BaseMessages.getString(PKG,
                "LucidDBStreamingLoaderMeta.Exception.TableNotFound"));
          }
        } else {
          throw new KettleException(BaseMessages.getString(PKG,
              "LucidDBStreamingLoaderMeta.Exception.TableNotSpecified"));
        }
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(PKG,
            "LucidDBStreamingLoaderMeta.Exception.ErrorGettingFields"), e);
      } finally {
        db.disconnect();
      }
    } else {
      throw new KettleException(BaseMessages.getString(PKG,
          "LucidDBStreamingLoaderMeta.Exception.ConnectionNotDefined"));
    }

  }

  /**
   * @return the schemaName
   */
  public String getSchemaName() {
    return schemaName;
  }

  /**
   * @param schemaName
   *          the schemaName to set
   */
  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public boolean[] getInsOrUptFlag() {
    return insOrUptFlag;
  }

  public void setInsOrUptFlag(boolean[] insOrUptFlag) {
    this.insOrUptFlag = insOrUptFlag;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String[] getFieldStreamForFields() {
    return fieldStreamForFields;
  }

  public void setFieldStreamForFields(String[] fieldStreamForFields) {
    this.fieldStreamForFields = fieldStreamForFields;
  }

  public String[] getFieldStreamForKeys() {
    return fieldStreamForKeys;
  }

  public void setFieldStreamForKeys(String[] fieldStreamForKeys) {
    this.fieldStreamForKeys = fieldStreamForKeys;
  }

  public String[] getFieldTableForFields() {
    return fieldTableForFields;
  }

  public void setFieldTableForFields(String[] fieldTableForFields) {
    this.fieldTableForFields = fieldTableForFields;
  }

  public String[] getFieldTableForKeys() {
    return fieldTableForKeys;
  }

  public void setFieldTableForKeys(String[] fieldTableForKeys) {
    this.fieldTableForKeys = fieldTableForKeys;
  }

  public String getCustom_sql() {
    return custom_sql;
  }

  public void setCustom_sql(String custom_sql) {
    this.custom_sql = custom_sql;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public SQLStatement getSQLStatements(TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev) throws KettleStepException {
    
    SQLStatement retval = super.getSQLStatements(transMeta, stepMeta, prev);
    
    if (databaseMeta!=null)
    {
      if (prev!=null && prev.size()>0)
      {
        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(
            transMeta.environmentSubstitute(schemaName), 
            transMeta.environmentSubstitute(tableName)
            );
        
        if (!Const.isEmpty(schemaTable))
        {
          Database db = new Database(loggingObject, databaseMeta);
          db.shareVariablesWith(transMeta);
          try
          {
            db.connect();
            
            String cr_table = db.getDDL(schemaTable, prev);
            
            // Empty string means: nothing to do: set it to null...
            if (cr_table==null || cr_table.length()==0) cr_table=null;
            
            retval.setSQL(cr_table);
          }
          catch(KettleDatabaseException dbe)
          {
            retval.setError(BaseMessages.getString(PKG, "LucidDBStreamingLoaderMeta.Error.ErrorConnecting", dbe.getMessage()));
          }
          finally
          {
            db.disconnect();
          }
        }
        else
        {
          retval.setError(BaseMessages.getString(PKG, "LucidDBStreamingLoaderMeta.Error.NoTable"));
        }
      }
      else
      {
        retval.setError(BaseMessages.getString(PKG, "LucidDBStreamingLoaderMeta.Error.NoInput"));
      }
    }
    else
    {
      retval.setError(BaseMessages.getString(PKG, "LucidDBStreamingLoaderMeta.Error.NoConnection"));
    }

    return retval;
  }

}
