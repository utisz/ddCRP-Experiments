#!/bin/bash

usage() { echo "Usage: $0 [-n  <num_iter> -v <vocab_size> -d <dirichlet_concentration_param> -s <self_linkage_prob> -t <crp_self_link_prob>]" 1>&2;
          echo -e  "\nnum_iter and vocab_size are required parameters. dirichlet_parameter is defaulted to 0.3 and self_linkage_prob is defaulted to 0.1"       

	exit 1; }


numIter=
vocabSize=
dir_param=0.3 #default value
self_link_prob=0.1 #default value
OUTPUTCSVFILE=./output.csv
while getopts ":n:v:d:s:t:h" o; do

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
javac -sourcepath src/ -d bin/ -cp "lib/la4j-0.4.9/bin/la4j-0.4.9.jar:lib/jgrapht-0.8.3/jgrapht-jdk1.6.jar:lib/commons-math3-3.2/commons-math3-3.2.jar" src/Driver.java

java -cp "lib/la4j-0.4.9/bin/la4j-0.4.9.jar:lib/jgrapht-0.8.3/jgrapht-jdk1.6.jar:lib/commons-math3-3.2/commons-math3-3.2.jar:bin" Driver $numIter $vocabSize $dir_param $self_link_prob $crp_self_link_prob
