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
#include <dirent.h>

/*************************************************************/
// PROTYPES
//


/*************************************************************/



int access(const char *pathname, int mode);
int is_file_exist(const char*file_path){
   if(file_path==NULL){
     return -1;
   }
   if(access(file_path,F_OK)==0){
    return 0;
   }
   return -1;
}
int is_dir_exist(const char*dir_path){
    if(dir_path==NULL){
        return -1;
    }
    if(opendir(dir_path)==NULL){
        return -1;
    }
    return 0;
}




int main(int argc, char *argv[])

{

    FILE *fd1;


	if (argc < 2) {
        printf(" Usage: %s outputFileName number number number etc. \n",argv[0]);
        return(1);
    }

    printf("file path = %s\n", argv[1]);

    int judgeFileResultCode=is_file_exist(argv[1]);
    if(judgeFileResultCode==0){
         printf("文件存在\n");
    }else if(judgeFileResultCode==-1){
         printf("文件不存在\n");
    }
    /*
    int judgeDirResultCode=is_dir_exist(argv[1]);
    if(judgeDirResultCode==0){
        printf("打开文件夹成功，这是个文件夹\n");
    }else if(judgeDirResultCode==-1){
        printf("打开文件夹失败，这不是个文件夹或者文件夹路径错误\n");
    }*/


    fd1 = fopen(argv[1],"rb");
    if (fd1 == NULL) {
        printf("error when openning file %s\n",argv[1]);
       return(1);
    }


    		//获取文件大小

		fseek(fd1 , 0 , SEEK_END);
		long lSize = ftell (fd1);
		printf("file size = %ld\n", lSize);
		rewind (fd1);

		/*
		long pos_start = ftell(fd1);//此处正常
        fseek(fd1, 0L, SEEK_END);
        long pos_end = ftell(fd1);//此处返回一个很大的值
        long file_data_size = pos_end - pos_start;
        */
		//开辟存储空间
		//int num = file_data_size/sizeof(unsigned int);
		int num = lSize/sizeof(unsigned int);
		unsigned int *pos = (unsigned int*) malloc (sizeof(unsigned int)*num);
		if (pos == NULL)
		{
			printf("开辟空间出错");
			return -1;
		}
		fread(pos,sizeof(unsigned int),num,fd1);
		for(int i = 0; i < num; i++)
			printf("%u\n", pos[i]);
		free(pos);     //释放内存



/*

    for (i = 2; i < argc; i++) {
        n = strtoul(argv[i],NULL,10);
        rc = fwrite(&n, sizeof(unsigned int), 1, fd1);
        if (rc != 1) {
            printf("error when writing to file %s \n",argv[1]);
            if (fd1 != NULL) fclose(fd1);
            return(1);
        }
    }
*/

    if (fd1 != NULL) fclose(fd1);

	exit(0);
}

