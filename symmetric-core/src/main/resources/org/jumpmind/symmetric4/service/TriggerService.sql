nameOfSomeSql: <
select sum(data_event_count), table_name from $(table)
  where status != 'OK'
  
complexQuerySql: <
select a, b, c, d, e, f, g, h from 
  alphabet where type_code='soup'
