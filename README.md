LBFGSonREEF
===========

Implement machine Learning altorithm LBFGS on REEF

Prerequisite
===========
* REEF 0.9-SNAPSHOT
* Wake 0.9-SNAPSHOT
* TANG 0.9-SNAPSHOT
* shimoga 0.1-SNAPSHOT

Build
===========
```
mvn clean install 
```

Working
===========
* Distributed group communication
 * ControllerTask scatters data to ComputeTasks
 * ComputeTasks compute according to the algorithm and reduce the result back
 * we just average the result data.(Since it is coefficient)
* Java ported l-bfgs algorithm

Not working
===========
* File loading api
 * I give up
 * I just put raw data on source code like MatMul example
* Logistic regression
 * l-bfgs algorithm is completely ported and working but failed to combine with Logistic Regresssion.
 * Should use appropriate sigmoid function, but I have no idea which function should I use.

Dataset
===========
A researcher is interested in how variables, such as GRE (Graduate Record Exam scores), GPA (grade point average) and prestige of the undergraduate institution, effect admission into graduate school. The response variable, admit/don't admit, is a binary variable.
* http://www.ats.ucla.edu/stat/data/binary.csv

Result
==========
STDOUT.txt from driver
```
****************** RESULT ******************
Controller merged data : 0.9999978847998146 0.9999957553215022 2.8
********************************************
```
Supposed to trained coefficient data. Currently No meaning since I didn't implement logistic regression well.

License of Libraries
==========
* liblbfgs
 * The MIT License
* REEF
 * Apache License, Version 2.0

References
==========
* L-BFGS
 * C port of LBFGS algorithm
  * http://www.chokkan.org/software/liblbfgs/
 * Notes on CG and LM-BFGS optimization of logistic regression
  * http://www.citeulike.org/user/rlsummerscales/article/9706575
* REEF
 * Group Communication(MatMul example)
  * https://github.com/Microsoft-CISL/shimoga/tree/master/src/main/java/com/microsoft/reef/examples/groupcomm/matmul
 
