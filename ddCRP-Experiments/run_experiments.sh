#!/bin/bash

usage() { echo "Usage: $0 [-n  <num_iter> -v <vocab_size> -d <dirichlet_concentration_param> -s <self_linkage_prob> -t <crp_self_link_prob>] -z {venue|space|cat} -m <num_samples> -e <num_experiments>" 1>&2;
          echo -e  "\nnum_iter and vocab_size are required parameters. dirichlet_parameter is defaulted to 0.3 and self_linkage_prob is defaulted to 0.1. num_samples is defaulted to 25 and num_experiments is defaulted to 1. Default sampling is across venue_ids"       

	exit 1; }
numIter=
vocabSize=
dir_param=0.3 #default value
self_link_prob=0.1 #default value
sample=1 #default value sample with venue_ids
sample_string="venues"
num_samples=25 #default value of number of samples from each city
num_experiments=1 #default value of number of experiments

OUTPUTCSVFILE=./output.csv
while getopts ":n:v:d:s:t:hz:e:m:" o; do

	case "${o}" in
		
		n)  
			numIter=$OPTARG
			;;
		v) 	
			vocabSize=$OPTARG
			;;
		d)
			dir_param=$OPTARG
			;;
		s)
			self_link_prob=$OPTARG
			;;
		t)
			crp_self_link_prob=$OPTARG
			;;
		z) 	if [ "$OPTARG" = "venue" ]; then
				sample=1
				sample_string="VENUE"
			elif [ "$OPTARG" = "space" ]; then
			       sample=2	
			       sample_string="SPACE"
		       elif [ "$OPTARG" = "cat" ]; then
			       sample=3
			       sample_string="CAT"
		       else
			       echo "Invalid sample parameter use {venue|space|cat}"
				exit 1;	
		       fi
			;;
		m)      num_samples=$OPTARG
			;;
		e)      num_experiments=$OPTARG
			;;
		h)
			usage;
			exit 1;
			;;
		\?) 
			echo "Invalid option -$OPTARG"
			exit 1;
			;;
		:)
			echo "Option -$OPTARG requires an argument." >&2
			exit 1;
			;;
	esac
done

if [ -z $numIter ];
then
	usage;
	exit 1;
fi
if [ -z $vocabSize ];
then
	usage;
	exit 1;
fi

if [ -f $OUTPUTCSVFILE ];
then
	rm $OUTPUTCSVFILE;
fi



mkdir tables
mkdir bin
echo "########  Running  $num_experiments experiments with sampling across $sample_string and sampling $num_samples venues for each city   ########### "
#Making directories for runs -Its of the form time_stamp/encoding of hyper_params/<run_id>

#First removing the LATEST dir
rm -rf run/LATEST
time_stamp=$(date +"%m-%d-%Y:%H:%M:%S")
DIR_NAME=run/$time_stamp/$dir_param/$self_link_prob/$crp_self_link_prob
mkdir -p $DIR_NAME
ln -s  `pwd`/run/$time_stamp `pwd`/run/LATEST


for (( i=1; i<=$num_experiments; i++ ))
do
	mkdir -p $DIR_NAME/$i
	echo "#######Starting to run $i trial###########"
javac -sourcepath src/ -d bin/ -cp "lib/la4j-0.4.9/bin/la4j-0.4.9.jar:lib/jgrapht-0.8.3/jgrapht-jdk1.6.jar:lib/commons-math3-3.2/commons-math3-3.2.jar" src/Driver.java
java -cp "lib/la4j-0.4.9/bin/la4j-0.4.9.jar:lib/jgrapht-0.8.3/jgrapht-jdk1.6.jar:lib/commons-math3-3.2/commons-math3-3.2.jar:bin" Driver $numIter $vocabSize $dir_param $self_link_prob $crp_self_link_prob $sample $num_samples $DIR_NAME/$i/
	echo "###### Finished running $i trials ########" 
done
