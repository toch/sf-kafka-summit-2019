package conf.kafkasummit.talk;

import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.Types;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.table.descriptors.Kafka;
import org.apache.flink.table.descriptors.Json;
import org.apache.flink.table.descriptors.Schema;

import java.util.Arrays;

public class StreamCrossingJob {
  public final static TypeInformation<Row> MOVIES_SCHEMA =
    Types.ROW(
      new String[]{"id", "name", "year"},
      new TypeInformation[]{
        BasicTypeInfo.INT_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO
      }
    );
  public final static TypeInformation<Row> GHOSTS_SCHEMA =
    Types.ROW(
      new String[]{"id", "name", "aka", "singular_or_multiple", "class_based_on_rpg", "class_in_materials"},
      new TypeInformation[]{
        BasicTypeInfo.INT_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO
      }
    );
  public final static TypeInformation<Row> GHOSTS_IN_MOVIES_SCHEMA =
    Types.ROW(
      new String[]{"ghost_id", "movie_id", "id"},
      new TypeInformation[]{
        BasicTypeInfo.INT_TYPE_INFO,
        BasicTypeInfo.INT_TYPE_INFO,
        BasicTypeInfo.INT_TYPE_INFO
      }
    );
  public static void main(String[] args) throws Exception {
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    final StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

    registerKafkaSource(tableEnv, "movies", MOVIES_SCHEMA);
    registerKafkaSource(tableEnv, "ghosts", GHOSTS_SCHEMA);
    registerKafkaSource(tableEnv, "ghosts_in_movies", GHOSTS_IN_MOVIES_SCHEMA);

    Table result = apply(tableEnv);
    
    registerKafkaSink(
      tableEnv,
      "ghost_appearances",
      result.getSchema()  
    );

    result.insertInto("ghost_appearances");

    env.execute("Cross the streams!");
  }

  private static void registerKafkaSource(StreamTableEnvironment tableEnv, String topic, TypeInformation<Row> typeinfo) {
    tableEnv
      .connect(
        new Kafka()
        .version("universal")
        .topic("ghostbusters_" + topic)
        .property("zookeeper.connect", "my-cp-zookeeper-headless:2181")
        .property("bootstrap.servers", "my-cp-kafka-headless:9092")
        .property("group.id", "flink")
        .startFromEarliest()
      )
      .withFormat(new Json().deriveSchema())
      .withSchema(new Schema().schema(TableSchema.fromTypeInfo(typeinfo)))
      .inAppendMode()
      .registerTableSource(topic);
  }

  private static void registerKafkaSink(StreamTableEnvironment tableEnv, String topic, TableSchema schema) {
    tableEnv
      .connect(
        new Kafka()
        .version("universal")
        .topic("ghostbusters_" + topic)
        .property("zookeeper.connect", "my-cp-zookeeper-headless:2181")
        .property("bootstrap.servers", "my-cp-kafka-headless:9092")
        .property("group.id", "flink")
        .startFromEarliest()
      )
      .withFormat(
        new Json()
        .deriveSchema()
      )
      .withSchema(new Schema().schema(schema))
      .inAppendMode()
      .registerTableSink(topic);
  }
  public static Table apply(TableEnvironment tableEnv) {
    return tableEnv.sqlQuery(
      "SELECT ghosts.name AS ghost, movies.name AS movie " +
      "FROM ghosts " +
      "JOIN ghosts_in_movies ON ghosts.id = ghosts_in_movies.ghost_id " +
      "JOIN movies ON movies.id = ghosts_in_movies.movie_id"
    );
  }
}