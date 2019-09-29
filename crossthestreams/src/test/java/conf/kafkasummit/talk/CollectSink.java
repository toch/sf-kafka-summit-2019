package conf.kafkasummit.talk;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.InputTypeConfigurable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.List;

// Mock Sink
public class CollectSink implements SinkFunction<Row>, InputTypeConfigurable {

  // must be static
  public static final List<Row> values = new ArrayList<>();
  public static RowTypeInfo schema = null;

  @Override
  public void setInputType(TypeInformation<?> type, ExecutionConfig executionConfig) {
    schema = (RowTypeInfo)type;
  }

  @Override
  public synchronized void invoke(Row value, SinkFunction.Context context) throws Exception {
      values.add(value);
  }
}