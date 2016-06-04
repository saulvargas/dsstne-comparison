#!/bin/bash

# download dataset
wget http://files.grouplens.org/datasets/movielens/ml-20m.zip

# decompress
unzip ml-20m.zip

# shuffle and partition
get_seeded_random()
{
  seed="$1"
  openssl enc -aes-256-ctr -pass pass:"$seed" -nosalt \
    </dev/zero 2>/dev/null
}
tail -n +2 ml-20m/ratings.csv | tr ',' '\t' | shuf --random-source=<(get_seeded_random 42) | split -l 4000053

# first subset as test, rest and partition
mv xaa test.data
cat xab xac xad xae > train.data

# removing unnecessary files
rm xab xac xad xae
rm -rf ml-20m

# auxiliary lists of users and items
cut -f1 train.data test.data | sort -u > users.txt
cut -f2 train.data test.data | sort -u > items.txt

# compile java project
mvn clean package

# run ranksys recommendations
java -Xmx6G -jar target/dsstne-comparison-0.1.jar compute

# convert training data to DSSTNE format
java -Xmx6G -jar target/dsstne-comparison-0.1.jar to-dsstne-format

# run DSSTNE with training data
generateNetCDF -d gl_input -i train.dsstne -o gl_input.nc -f features_input -s samples_input -c
generateNetCDF -d gl_output -i train.dsstne -o gl_output.nc -f features_output -s samples_input -c
wget https://s3-us-west-2.amazonaws.com/amazon-dsstne-samples/configs/config.json
train -c config.json -i gl_input.nc -o gl_output.nc -n gl.nc -b 256 -e 10
predict -b 1024 -d gl -i features_input -o features_output -k 100 -n gl.nc -f train.dsstne -s predictions.dsstne -r train.dsstne

# convert generated recommendations to ranksys format
java -Xmx6G -jar target/dsstne-comparison-0.1.jar from-dsstne-format < predictions.dsstne > dsstne

# evaluate recommendations
java -Xmx6G -jar target/dsstne-comparison-0.1.jar evaluate pop rnd mf ub dsstne

