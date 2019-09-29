package conf.kafkasummit.talk;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.Types;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import org.apache.flink.test.util.AbstractTestBase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class StreamCrossingJobTest extends AbstractTestBase {
  private StreamExecutionEnvironment env;
  private StreamTableEnvironment tableEnv;
  private static CollectSink collectSink;

  @Before
  public void before() {
    env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.setParallelism(1);

    tableEnv = StreamTableEnvironment.create(env);

    // values are collected in a static variable to address Flink serialization of all operators
    collectSink.values.clear();
  }

  private DataStream<Row> moviesFixture() {
    return env
    .fromElements(Row.of(1, "Ghostbusters", "1984"))
    .returns(StreamCrossingJob.MOVIES_SCHEMA);
  }

  private DataStream<Row> ghostsFixture() {
    return env
    .fromElements(
      Row.of(2, "Slimer", "Onionhead; Green Ghost", "Singular Entity", "Class 5", "Class 5")
    ).returns(StreamCrossingJob.GHOSTS_SCHEMA);
  }

  private DataStream<Row> ghostsInMoviesFixture() {
    return env
    .fromElements(Row.of(2, 1, 2))
    .returns(StreamCrossingJob.GHOSTS_IN_MOVIES_SCHEMA);
  }
  

  @Test
  public void itCrossesStreams() throws Exception {
    tableEnv.registerDataStream("movies", moviesFixture());
    tableEnv.registerDataStream("ghosts", ghostsFixture());
    tableEnv.registerDataStream("ghosts_in_movies", ghostsInMoviesFixture());
    
    
    Table result = StreamCrossingJob.apply(tableEnv);

    TableSchema tableSchema = result.getSchema();
    DataStream<Row> output = tableEnv.toAppendStream(result, tableSchema.toRowType());
    
    output.addSink(collectSink = new CollectSink());
    
    env.execute();

    List<Row> expectedRowList = buildRowList(new Object[][] {
      new Object[]{"Slimer", "Ghostbusters"},
    });;

    assertEquals(expectedRowList, collectSink.values);
  }

  private Row buildRow(Object[] values) {
    Row row = new Row(values.length);
    IntStream.range(0, values.length)
             .forEach(idx -> row.setField(idx, values[idx]));
    return row;
  }

  private List<Row> buildRowList(Object[][] records) {
    List<Row> rowList = new ArrayList<Row>();

    for (Object[] row : records) {
      rowList.add(buildRow(row));
    }

    return rowList;
  }
}