/*
 * Copyright 2017 PingCAP, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pingcap.tikv.catalog;

import com.google.common.collect.ImmutableList;
import com.pingcap.tikv.KVMockServer;
import com.pingcap.tikv.PDMockServer;
import com.pingcap.tikv.TiCluster;
import com.pingcap.tikv.TiConfiguration;
import com.pingcap.tikv.kvproto.Kvrpcpb.IsolationLevel;
import com.pingcap.tikv.meta.MetaUtils.MetaMockHelper;
import com.pingcap.tikv.meta.TiDBInfo;
import com.pingcap.tikv.meta.TiTableInfo;
import com.pingcap.tikv.region.TiRegion;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class CatalogTransactionTest {
  private KVMockServer kvServer;
  private PDMockServer pdServer;
  private static final long CLUSTER_ID = 1024;
  private TiConfiguration conf;

  @Before
  public void setUp() throws Exception {
    pdServer = new PDMockServer();
    pdServer.start(CLUSTER_ID);
    kvServer = new KVMockServer();
    kvServer.start(new TiRegion(MetaMockHelper.region, MetaMockHelper.region.getPeers(0), IsolationLevel.RC));
    // No PD needed in this test
    conf = TiConfiguration.createDefault(ImmutableList.of("127.0.0.1:" + pdServer.port));
  }

  @Test
  public void getLatestSchemaVersionTest() throws Exception {
    MetaMockHelper helper = new MetaMockHelper(pdServer, kvServer);
    helper.preparePDForRegionRead();
    helper.setSchemaVersion(666);
    TiCluster cluster = TiCluster.getCluster(conf);
    CatalogTransaction trx = new CatalogTransaction(cluster.createSnapshot());
    assertEquals(666, trx.getLatestSchemaVersion());
  }

  @Test
  public void getDatabasesTest() throws Exception {
    MetaMockHelper helper = new MetaMockHelper(pdServer, kvServer);
    helper.preparePDForRegionRead();
    helper.addDatabase(130, "global_temp");
    helper.addDatabase(264, "TPCH_001");

    TiCluster cluster = TiCluster.getCluster(conf);
    CatalogTransaction trx = new CatalogTransaction(cluster.createSnapshot());
    List<TiDBInfo> dbs = trx.getDatabases();
    assertEquals(2, dbs.size());
    assertEquals(130, dbs.get(0).getId());
    assertEquals("global_temp", dbs.get(0).getName());

    assertEquals(264, dbs.get(1).getId());
    assertEquals("TPCH_001", dbs.get(1).getName());

    TiDBInfo db = trx.getDatabase(130);
    assertEquals(130, db.getId());
    assertEquals("global_temp", db.getName());
  }

  @Test
  public void getTablesTest() throws Exception {
    MetaMockHelper helper = new MetaMockHelper(pdServer, kvServer);
    helper.preparePDForRegionRead();
    helper.addTable(130, 42, "test");
    helper.addTable(130, 43, "tEst1");

    TiCluster cluster = TiCluster.getCluster(conf);
    CatalogTransaction trx = new CatalogTransaction(cluster.createSnapshot());
    List<TiTableInfo> tables = trx.getTables(130);
    assertEquals(tables.size(), 2);
    assertEquals(tables.get(0).getName(), "test");
    assertEquals(tables.get(1).getName(), "tEst1");
  }
}