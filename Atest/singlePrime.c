
// INCLUDE FILES
//
#include "stdio.h"
#include "stdlib.h"
#include <unistd.h>
#include <dirent.h>

#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>


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

int morph(char *number){
    //if(execv("/home/inzahgi/code/xx/isPrime", number)<0){
    printf("line 36 number = %s$", number);
    char *p[] = {"/home/inzahgi/code/xx/testPrime", number, NULL};
    if(execv("/home/inzahgi/code/xx/testPrime", p)<0){
        perror("Error on execv");
                return -1;
    }
    return -1;
}




int main(int argc, char *argv[])
{

    FILE *fd1;
    unsigned int first_num;


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

		//开辟存储空间
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


        char s[10];
        sprintf(s,"%d",pos[0]);
        free(pos);     //释放内存

        return morph(s);

}

