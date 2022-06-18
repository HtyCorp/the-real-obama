package io.mamish.therealobama.dao;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.stream.Collectors;

public class WordMetadataDao {

    private static final String METADATA_TABLE_NAME = "WordMetadata";

    private final DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(DynamoDbClient.create())
            .build();

    private final DynamoDbTable<WordMetadataItem> metadataTable = ddbClient.table(METADATA_TABLE_NAME, TableSchema.fromBean(WordMetadataItem.class));

    public List<WordMetadataItem> queryWordMetadata(String word) {
        return metadataTable.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(word).build()))
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    public void putWordMetadata(WordMetadataItem item) {
        metadataTable.putItem(item);
    }

}
