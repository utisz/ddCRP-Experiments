while read line
do
	echo $line | tr ' ' '\n' | wc -l
done< corpus.txt
