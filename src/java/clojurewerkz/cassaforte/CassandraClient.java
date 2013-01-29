package clojurewerkz.cassaforte;

import org.apache.cassandra.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * CassandraClient base class for the driver, performs all communication with
 * Cassandra server.
 */
public class CassandraClient {

  private Cassandra.Client client;
  private TTransport transport;

  private static final int DEFAULT_PORT = 9160;
  private int port;
  private String hostname;
  private static final String DEFAULT_CQL_VERSION = "3.0.0";


  //
  // Constructors
  //

  /**
   *
   * @param thriftClient
   * @param thriftTransport
   */
  public CassandraClient(Cassandra.Client thriftClient, TTransport thriftTransport) {
    client = thriftClient;
    transport = thriftTransport;
  }

  /**
   *
   * @param hostname
   * @throws TException
   * @throws InvalidRequestException
   */
  public CassandraClient(String hostname) throws TException, InvalidRequestException {
    this(hostname, DEFAULT_PORT);
  }

  /**
   *
   * @param hostname
   * @param port
   * @throws TException
   * @throws InvalidRequestException
   */
  public CassandraClient(String hostname, int port) throws TException, InvalidRequestException {
    this.hostname = hostname;
    this.port = port;

    TTransport tr = new TFramedTransport(new TSocket(hostname, port));

    transport = tr;
    client = new Cassandra.Client(new TBinaryProtocol(tr));
    tr.open();
    client.set_cql_version(DEFAULT_CQL_VERSION);
  }

  //
  // API
  //

  /**
   *
   * @return
   */
  public String getHostname() {
    return this.hostname;
  }

  /**
   *
   * @return
   */
  public String getHost() {
    return this.hostname;
  }

  /**
   *
   * @return
   */
  public int getPort() {
    return this.port;
  }

  /**
   * Executes a CQL query, uses no compression
   *
   * @param query Query to execute
   * @return CQL query result
   */
  public CqlResult executeCqlQuery(String query) throws TException, TimedOutException, SchemaDisagreementException, InvalidRequestException, UnavailableException, UnsupportedEncodingException {
    return executeCqlQuery(query, Compression.NONE);
  }

  /**
   * Executes a CQL query, uses specified compression strategy
   *
   * @param query Query to execute
   * @return CQL query result
   */
  public CqlResult executeCqlQuery(String query, Compression compression) throws UnsupportedEncodingException, TException, TimedOutException, SchemaDisagreementException, InvalidRequestException, UnavailableException {
    return client.execute_cql_query(ByteBuffer.wrap(query.getBytes("UTF-8")), compression);
  }

  //
  // Thrift Client delegates
  //

  /**
   * Logs current client to Cassandra client with given AuthenticationRequest
   *
   * @param auth_request authentication request params
   * @throws AuthenticationException
   * @throws AuthorizationException
   * @throws TException
   */
  public void login(AuthenticationRequest auth_request) throws AuthenticationException, AuthorizationException, TException {
    client.login(auth_request);
  }

  /**
   * Sets current keyspace
   *
   * @param keyspace keyspace to set
   * @throws InvalidRequestException
   * @throws TException
   */
  public void set_keyspace(String keyspace) throws InvalidRequestException, TException {
    client.set_keyspace(keyspace);
  }

  /**
   * Gets Column or SuperColumn by given path from Cassandra store
   *
   * @param key               - key of the required Column or SuperCOlumn
   * @param column_path       -
   * @param consistency_level
   * @return
   * @throws InvalidRequestException
   * @throws NotFoundException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public ColumnOrSuperColumn get(ByteBuffer key, ColumnPath column_path, ConsistencyLevel consistency_level) throws InvalidRequestException, NotFoundException, UnavailableException, TimedOutException, TException {
    return client.get(key, column_path, consistency_level);
  }

  /**
   *
   * @param key
   * @param column_parent
   * @param predicate
   * @param consistency_level
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public int get_count(ByteBuffer key, ColumnParent column_parent, SlicePredicate predicate, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    return client.get_count(key, column_parent, predicate, consistency_level);
  }

  /**
   *
   * @param key
   * @param column_parent
   * @param predicate
   * @param consistency_level
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public List<ColumnOrSuperColumn> get_slice(ByteBuffer key, ColumnParent column_parent, SlicePredicate predicate, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    return client.get_slice(key, column_parent, predicate, consistency_level);
  }

  /**
   *
   * @param keys
   * @param column_parent
   * @param predicate
   * @param consistency_level
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public Map<ByteBuffer, Integer> multiget_count(List<ByteBuffer> keys, ColumnParent column_parent, SlicePredicate predicate, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    return client.multiget_count(keys, column_parent, predicate, consistency_level);
  }

  /**
   *
   * @param keys
   * @param column_parent
   * @param predicate
   * @param consistency_level
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public Map<ByteBuffer, List<ColumnOrSuperColumn>> multiget_slice(List<ByteBuffer> keys, ColumnParent column_parent, SlicePredicate predicate, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    return client.multiget_slice(keys, column_parent, predicate, consistency_level);
  }

  /**
   * Inserts Column by given ColumnParent with a given Key and ConsistencyLevel.
   *
   * @param key
   * @param column_parent
   * @param column
   * @param consistency_level
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public void insert(ByteBuffer key, ColumnParent column_parent, Column column, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    client.insert(key, column_parent, column, consistency_level);
  }

  /**
   *
   * @param column_parent
   * @param predicate
   * @param range
   * @param consistency_level
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public List<KeySlice> get_range_slices(ColumnParent column_parent, SlicePredicate predicate, KeyRange range, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    return client.get_range_slices(column_parent, predicate, range, consistency_level);
  }

  /**
   *
   * @param column_family
   * @param range
   * @param start_column
   * @param consistency_level
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public List<KeySlice> get_paged_slice(String column_family, KeyRange range, ByteBuffer start_column, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    return client.get_paged_slice(column_family, range, start_column, consistency_level);
  }

  /**
   *
   * @param column_parent
   * @param index_clause
   * @param column_predicate
   * @param consistency_level
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public List<KeySlice> get_indexed_slices(ColumnParent column_parent, IndexClause index_clause, SlicePredicate column_predicate, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    return client.get_indexed_slices(column_parent, index_clause, column_predicate, consistency_level);
  }

  /**
   *
   * @param query
   * @param compression
   * @return
   * @throws InvalidRequestException
   * @throws TException
   */
  public CqlPreparedResult prepare_cql_query(ByteBuffer query, Compression compression) throws InvalidRequestException, TException {
    return client.prepare_cql_query(query, compression);
  }

  /**
   *
   * @param itemId
   * @param values
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws SchemaDisagreementException
   * @throws TException
   */
  public CqlResult execute_prepared_cql_query(int itemId, List<ByteBuffer> values) throws InvalidRequestException, UnavailableException, TimedOutException, SchemaDisagreementException, TException {
    return client.execute_prepared_cql_query(itemId, values);
  }

  /**
   *
   * @param query
   * @param compression
   * @return
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws SchemaDisagreementException
   * @throws TException
   */
  public CqlResult execute_cql_query(ByteBuffer query, Compression compression) throws InvalidRequestException, UnavailableException, TimedOutException, SchemaDisagreementException, TException {
    return client.execute_cql_query(query, compression);
  }

  public CqlResult execute_cql3_query(ByteBuffer query, Compression compression, ConsistencyLevel consistency)  throws InvalidRequestException, UnavailableException, TimedOutException, SchemaDisagreementException, org.apache.thrift.TException {
    return client.execute_cql3_query(query, compression, consistency);
  }

  /**
   *
   * @return
   * @throws TException
   */
  public String describe_version() throws TException {
    return client.describe_version();
  }

  /**
   *
   * @return
   * @throws TException
   */
  public String describe_snitch() throws TException {
    return client.describe_snitch();
  }

  /**
   *
   * @return
   * @throws TException
   */
  public String describe_cluster_name() throws TException {
    return client.describe_cluster_name();
  }

  /**
   *
   * @param keyspace
   * @return
   * @throws NotFoundException
   * @throws InvalidRequestException
   * @throws TException
   */
  public KsDef describe_keyspace(String keyspace) throws NotFoundException, InvalidRequestException, TException {
    return client.describe_keyspace(keyspace);
  }

  /**
   *
   * @return
   * @throws InvalidRequestException
   * @throws TException
   */
  public List<KsDef> describe_keyspaces() throws InvalidRequestException, TException {
    return client.describe_keyspaces();
  }

  /**
   *
   * @return
   * @throws TException
   */
  public String describe_partitioner() throws TException {
    return client.describe_partitioner();
  }

  /**
   *
   * @param keyspace
   * @return
   * @throws InvalidRequestException
   * @throws TException
   */
  public List<TokenRange> describe_ring(String keyspace) throws InvalidRequestException, TException {
    return client.describe_ring(keyspace);
  }

  /**
   *
   * @return
   * @throws InvalidRequestException
   * @throws TException
   */
  public Map<String, List<String>> describe_schema_versions() throws InvalidRequestException, TException {
    return client.describe_schema_versions();
  }

  /**
   *
   * @param cfName
   * @param start_token
   * @param end_token
   * @param keys_per_split
   * @return
   * @throws InvalidRequestException
   * @throws TException
   */
  public List<String> describe_splits(String cfName, String start_token, String end_token, int keys_per_split) throws InvalidRequestException, TException {
    return client.describe_splits(cfName, start_token, end_token, keys_per_split);
  }

  /**
   *
   * @param key
   * @param column_parent
   * @param column
   * @param consistency_level
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public void add(ByteBuffer key, ColumnParent column_parent, CounterColumn column, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    client.add(key, column_parent, column, consistency_level);
  }

  /**
   *
   * @param mutation_map
   * @param consistency_level
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public void batch_mutate(Map<ByteBuffer, Map<String, List<Mutation>>> mutation_map, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    client.batch_mutate(mutation_map, consistency_level);
  }

  /**
   *
   * @param key
   * @param column_path
   * @param timestamp
   * @param consistency_level
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public void remove(ByteBuffer key, ColumnPath column_path, long timestamp, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    client.remove(key, column_path, timestamp, consistency_level);
  }

  /**
   *
   * @param key
   * @param path
   * @param consistency_level
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public void remove_counter(ByteBuffer key, ColumnPath path, ConsistencyLevel consistency_level) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    client.remove_counter(key, path, consistency_level);
  }

  /**
   *
   * @param cf_def
   * @return
   * @throws InvalidRequestException
   * @throws SchemaDisagreementException
   * @throws TException
   */
  public String system_add_column_family(CfDef cf_def) throws InvalidRequestException, SchemaDisagreementException, TException {
    return client.system_add_column_family(cf_def);
  }

  /**
   *
   * @param ks_def
   * @return
   * @throws InvalidRequestException
   * @throws SchemaDisagreementException
   * @throws TException
   */
  public String system_add_keyspace(KsDef ks_def) throws InvalidRequestException, SchemaDisagreementException, TException {
    return client.system_add_keyspace(ks_def);
  }

  /**
   *
   * @param column_family
   * @return
   * @throws InvalidRequestException
   * @throws SchemaDisagreementException
   * @throws TException
   */
  public String system_drop_column_family(String column_family) throws InvalidRequestException, SchemaDisagreementException, TException {
    return client.system_drop_column_family(column_family);
  }

  /**
   *
   * @param keyspace
   * @return
   * @throws InvalidRequestException
   * @throws SchemaDisagreementException
   * @throws TException
   */
  public String system_drop_keyspace(String keyspace) throws InvalidRequestException, SchemaDisagreementException, TException {
    return client.system_drop_keyspace(keyspace);
  }

  /**
   *
   * @param cf_def
   * @return
   * @throws InvalidRequestException
   * @throws SchemaDisagreementException
   * @throws TException
   */
  public String system_update_column_family(CfDef cf_def) throws InvalidRequestException, SchemaDisagreementException, TException {
    return client.system_update_column_family(cf_def);
  }

  /**
   *
   * @param ks_def
   * @return
   * @throws InvalidRequestException
   * @throws SchemaDisagreementException
   * @throws TException
   */
  public String system_update_keyspace(KsDef ks_def) throws InvalidRequestException, SchemaDisagreementException, TException {
    return client.system_update_keyspace(ks_def);
  }

  /**
   *
   * @param cfname
   * @throws InvalidRequestException
   * @throws UnavailableException
   * @throws TimedOutException
   * @throws TException
   */
  public void truncate(String cfname) throws InvalidRequestException, UnavailableException, TimedOutException, TException {
    client.truncate(cfname);
  }


  //
  // Transport delegates
  //

  /**
   *
   * @throws TTransportException
   */
  public void open() throws TTransportException {
    transport.open();
  }

  /**
   *
   */
  public void close() {
    transport.close();
  }

  /**
   *
   * @throws TTransportException
   */
  public void flush() throws TTransportException {
    transport.flush();
  }

  /**
   *
   * @return
   */
  public boolean isOpen() {
    return transport.isOpen();
  }
}
