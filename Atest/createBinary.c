/* 
File is createBinary.c

Purpose:
a program that creates a binary file for testing the prime number program 

input:
a sequence of numbers to be tested

output:
0 - if success
1 - if an error occured
a binary file with the name.   

Assumption:
1. the program does not check if the number is a positive integer
2. the program overwrites that file testPrime.bin


Notice:
Code can be used only for the assignment.  Other usages are not allowed

Copyright 2018 Doron Nussbaum
*/ 

/**************************************************************/
// INCLUDE FILES
//
#include "stdio.h"
#include "stdlib.h"
#include <unistd.h>


/*************************************************************/
// PROTYPES
//


/*************************************************************/


int main(int argc, char *argv[])

{
	int i;
    FILE *fd1;
    char s[32];
    unsigned int n;
    int rc = 0;

	unsigned int number;

	if (argc < 2) {
        printf(" Usage: %s outputFileName number number number etc. \n",argv[0]);
        return(1);
    }

    fd1 = fopen(argv[1],"wb");
    if (fd1 == NULL) {
        printf("error when openning file %s\n",argv[1]);
       return(1);
    }

    for (i = 2; i < argc; i++) {
        n = strtoul(argv[i],NULL,10);
        rc = fwrite(&n, sizeof(unsigned int), 1, fd1);
        if (rc != 1) {
            printf("error when writing to file %s \n",argv[1]);
            if (fd1 != NULL) fclose(fd1);
            return(1);
        }
    }


    if (fd1 != NULL) fclose(fd1);

	exit(0);
}

