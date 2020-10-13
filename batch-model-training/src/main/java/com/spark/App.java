package com.spark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.classification.LogisticRegressionModel;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;

public class App {
    public static void main(String[] args) {
        System.out.println("Model training started");
        if (args.length < 1) {
            throw new IllegalArgumentException("Csv file path on the hdfs must be passed as argument");
        }

        String sparkMasterUrl = System.getenv("SPARK_MASTER_URL");
        String hdfsUrl = System.getenv("HDFS_URL");
        String indexersHdfsPath = System.getenv("INDEXERS_PATH");
        String modelHdfsPath = System.getenv("MODEL_PATH");
        String dataUrl = args[0];

        if (isNullOrEmpty(sparkMasterUrl)) {
            throw new IllegalStateException("SPARK_MASTER_URL environment variable must be set.");
        }
        if (isNullOrEmpty(hdfsUrl)) {
            throw new IllegalStateException("HDFS_URL environment variable must be set");
        }
        if (isNullOrEmpty(indexersHdfsPath)) {
            throw new IllegalStateException("HDFS_URL environment variable must be set");
        }
        if (isNullOrEmpty(modelHdfsPath)) {
            throw new IllegalStateException("HDFS_URL environment variable must be set");
        }

        String csvFileUrl = hdfsUrl + dataUrl;
        String indexerPath = hdfsUrl + indexersHdfsPath;
        String modelPath = hdfsUrl + modelHdfsPath;

        SparkSession spark = SparkSession.builder().appName("Acciedents analysis").master(sparkMasterUrl).getOrCreate();
        System.out.println("Spark context created");
        
        Dataset<Row> dataset = spark.read().option("header", "true").csv(csvFileUrl);
        System.out.println("Dataset loaded");

        Dataset<Row> selectedColumns = dataset.select(
                dataset.col("TMC").cast(DataTypes.DoubleType), 
                dataset.col("Severity").cast(DataTypes.DoubleType),
                dataset.col("Distance(mi)").cast(DataTypes.DoubleType), 
                dataset.col("Street"), 
                dataset.col("City"), 
                dataset.col("State"),
                dataset.col("Country"), 
                dataset.col("Temperature(F)").cast(DataTypes.DoubleType), 
                dataset.col("Wind_Chill(F)").cast(DataTypes.DoubleType),
                dataset.col("Humidity(%)").cast(DataTypes.DoubleType), 
                dataset.col("Pressure(in)").cast(DataTypes.DoubleType), 
                dataset.col("Visibility(mi)").cast(DataTypes.DoubleType),
                dataset.col("Wind_Speed(mph)").cast(DataTypes.DoubleType), 
                dataset.col("Precipitation(in)").cast(DataTypes.DoubleType), 
                dataset.col("Weather_Condition")
            );

        Dataset<Row> filtered = selectedColumns.filter((row) -> {
            return !row.anyNull();
        })// Row count: 560721
        .sample(0.02); 
        System.out.println("SELECTED COLUMNS AND FILTERED ROWS");

        StringIndexerModel streetIndexer = new StringIndexer()
            .setInputCol("Street")
            .setOutputCol("StreetIndex")
            .fit(filtered);
        StringIndexerModel cityIndexer = new StringIndexer()
            .setInputCol("City")
            .setOutputCol("CityIndex")
            .fit(filtered);
        StringIndexerModel stateIndexer = new StringIndexer()
            .setInputCol("State")
            .setOutputCol("StateIndex")
            .fit(filtered);
        StringIndexerModel countryIndexer = new StringIndexer()
            .setInputCol("Country")
            .setOutputCol("CountryIndex")
            .fit(filtered);
        StringIndexerModel weatherIndexer = new StringIndexer()
            .setInputCol("Weather_Condition")
            .setOutputCol("WeatherConditionIndex")
            .fit(filtered);

        StringIndexerModel[] indexerArray = { streetIndexer, cityIndexer, stateIndexer, countryIndexer, weatherIndexer };
        System.out.println("Indexers created");

        Pipeline indexerPipeline = new Pipeline().setStages(indexerArray);
        Dataset<Row> transfromed = indexerPipeline.fit(filtered).transform(filtered);
        System.out.println("Indexer pipeline applied");

        saveIndexersOnHDFS(indexerPath, indexerArray);
        System.out.println("Indexers saved");

        String[] columnsToRemove = new String[]{"TMC","Severity","Street","City","State","Country","Weather_Condition","StreetIndex"} ;
        String labelCol = "Severity";
        String features = "Features";
        
        String[] featureCols = removeColumns(transfromed.columns(), columnsToRemove);

        System.out.println("FEATURES: " + Arrays.toString(featureCols));
        VectorAssembler vectorAssembler = new VectorAssembler()
            .setInputCols(featureCols)
            .setOutputCol(features);

        Dataset<Row>[] splits = vectorAssembler
            .transform(transfromed)
            .randomSplit(new double[] {0.7, 0.3});
        Dataset<Row> trainingSet = splits[0];
        Dataset<Row> testSet = splits[1];
        
        trainingSet.printSchema();
        trainingSet.show(100);

        System.out.println("Creating Random Forest model");
        RandomForestClassifier rf = new RandomForestClassifier()
            .setLabelCol(labelCol)
            .setFeaturesCol(features)
            .setMaxBins(65832);
        
        System.out.println("TRAINING MODEL");
        RandomForestClassificationModel model = rf.fit(trainingSet);

        // System.out.println("Creaeting Logistic Regression model");
        // LogisticRegression lr = new LogisticRegression()
        //     .setLabelCol(labelCol)
        //     .setFeaturesCol(features);
        // System.out.println("Training LR model");
        // LogisticRegressionModel model = lr.fit(trainingSet);
            

        System.out.println("Saving model");
        try {
            model.write().overwrite().save(modelPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("TESTING MODEL");
        Dataset<Row> predictions = model.transform(testSet);
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
            .setLabelCol(labelCol)
            .setPredictionCol("prediction")
            .setMetricName("accuracy");
        
        double accuracy = evaluator.evaluate(predictions);
        System.out.println("Model accuracy " + accuracy);

        spark.stop();
        spark.close();
    }

    private static void saveIndexersOnHDFS(String path, StringIndexerModel[] indexers) {

        for (StringIndexerModel sim : indexers) {
            String col = sim.getInputCol();
            String savePath = path + col;

            try {
                sim.write().overwrite().save(savePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String[] removeColumns(String[] columns, String[] colsToRemove) {

        List<String> colsList = new ArrayList<String>(Arrays.asList(columns));
        List<String> toRemove = Arrays.asList(colsToRemove);

        colsList.removeAll(toRemove);
        
        return colsList.toArray(new String[0]);
    }

    static boolean isNullOrEmpty(String str)
    {
        return str == null || str.isEmpty();
    }
}
