docker exec -it namenode hdfs dfs -test -e /big-data
if [ $? -eq 1 ]
then
  echo "[INFO]: Creating /big-data folder on HDFS"
  docker exec -it namenode hdfs dfs -mkdir /big-data
fi

docker exec -it namenode hdfs dfs -test -e /big-data/data.csv
if [ $? -eq 1 ]
then
  echo "[INFO]: Adding data.csv in the /big-data folder on the HDFS"
  docker exec -it namenode hdfs dfs -put /big-data/data.csv /big-data/data.csv
fi

docker exec -it namenode hdfs dfs -test -e /big-data/events.csv
if [ $? -eq 1 ]
then
  echo "[INFO]: Adding events.csv in the /big-data folder on the HDFS"
  docker exec -it namenode hdfs dfs -put /big-data/events.csv /big-data/events.csv
fi