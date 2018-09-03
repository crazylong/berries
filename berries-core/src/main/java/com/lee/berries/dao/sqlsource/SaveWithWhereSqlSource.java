package com.lee.berries.dao.sqlsource;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMapping.Builder;
import org.apache.ibatis.session.Configuration;

import com.lee.berries.dao.params.SaveWithWhereParam;

public class SaveWithWhereSqlSource extends BaseSqlSource {

	private Map<String, Object> parameterObject;
	private final static String SQL = "insert into {tableName} ({fields}) select {values} from dual where not exists (select 1 from {tableName} where 1=1 {where})";

	public SaveWithWhereSqlSource(Configuration configuration, Object object) {
		this.configuration = configuration;
		initSQL(object);
	}
	
	private String initSQL(Object object){
		parameterObject = new HashMap<String,Object>();
		SaveWithWhereParam<?> param = (SaveWithWhereParam<?>) object;
		parameterMappings = new ArrayList<ParameterMapping>();
		String sql = SQL;
		sql = sql.replace("{tableName}", tableNameProvider.getTableName(param.getTarget().getClass()));
		try{
			StringBuilder fields = new StringBuilder(100);
			StringBuilder values = new StringBuilder(100);
			StringBuilder wheres = new StringBuilder(300);
			for(Field field:param.getTarget().getClass().getDeclaredFields()){
				if(!field.getName().equals("serialVersionUID")){
					field.setAccessible(true);
					Object value = field.get(param.getTarget());
					if(value != null){
						String column = columnNameProvider.getColumnName(field.getName());
						fields.append(", ");
						fields.append(column);
						
						values.append(",'");
						values.append(getValue(value));
						values.append("'");
						
						addWhere(wheres, field, value, param.getWhereFields());
					}
				}
			}
			
			
			sql = sql.replace("{fields}", fields.substring(1));
			sql = sql.replace("{values}", values.substring(1));
			sql = sql.replace("{where}", wheres.toString());
			this.boundSql = new BoundSql(configuration, sql, parameterMappings, object);
		}
		catch(Exception e){
			e.printStackTrace();
			//防御性容错
		}
		return sql;
	}
	
	private Object getValue(Object value) {
		if(value instanceof Boolean) {
			Boolean booleanValue = (Boolean) value;
			return booleanValue? 1:0;
		}
		if(value instanceof Date) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(((Date)value));
		}
		return value;
	}
	
	private void addWhere(StringBuilder where, Field field, Object value, String[] whereFields) {
		for(String whereFieldName:whereFields) {
			if(whereFieldName.equals(field.getName())) {
				String key = whereFieldName;
				String column = columnNameProvider.getColumnName(field.getName());
				where.append(" and ");
				where.append(column);
				where.append(" =? ");
				Builder builder = new Builder(configuration, key, field.getType());
				ParameterMapping parameterMapping = builder.build();
				parameterMappings.add(parameterMapping);
				parameterObject.put(key, value);
				break;
			}
		}
	}
	
	@Override
	public Object getParamObject() {
		return parameterObject;
	}
}
