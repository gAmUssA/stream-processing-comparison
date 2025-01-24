package com.example.streaming.kafka;

import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.LRUCache;
import org.rocksdb.Options;

import java.util.Map;

public class CustomRocksDBConfig implements RocksDBConfigSetter {
    @Override
    public void setConfig(final String storeName, final Options options,
                         final Map<String, Object> configs) {
        // Get the existing table config instead of creating a new one
        BlockBasedTableConfig tableConfig = (BlockBasedTableConfig) options.tableFormatConfig();
        
        // Optimize for point lookups
        //tableConfig.setBlockCacheSize(50 * 1024 * 1024L); // 50MB block cache
        tableConfig.setBlockCache(new LRUCache(50 * 1024 * 1024L)); // 50MB block cache
        tableConfig.setCacheIndexAndFilterBlocks(true);
        
        options.setTableFormatConfig(tableConfig);
        
        // Optimize write buffer
        options.setWriteBufferSize(8 * 1024 * 1024L); // 8MB write buffer
        
        // Enable bloom filters for better read performance
        options.setMaxOpenFiles(-1); // Keep all files open
        options.setMaxWriteBufferNumber(3);
        options.setMinWriteBufferNumberToMerge(1);
    }

    @Override
    public void close(final String storeName, final Options options) {
        // Clean up RocksDB resources
        options.close();
    }
}
