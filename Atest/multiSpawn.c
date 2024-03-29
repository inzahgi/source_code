
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
int is_dir_exist(const char*dir_path){
    if(dir_path==NULL){
        return -1;
    }
    if(opendir(dir_path)==NULL){
        return -1;
    }
    return 0;
}

int morph(char *number){
    //if(execv("/home/inzahgi/code/xx/isPrime", number)<0){
    printf("line 36 number = %s$", number);
    char *p[] = {"/home/inzahgi/code/xx/isPrime", number, NULL};
    if(execv("/home/inzahgi/code/xx/isPrime", p)<0){
        perror("Error on execv");
                return -1;
    }
    return -1;
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


		int cnt = 0;
		for(cnt = 0; cnt < 10 && cnt < num; cnt++){
            fread(pos,sizeof(unsigned int),num,fd1);
            printf("%d----- %u\n", cnt, pos[cnt]);
		}

    if (fd1 != NULL) fclose(fd1);






    //pid_t pid;
    pid_t pw[cnt];
    int status;
    for(int i = 0; i< cnt; i++){
        pid_t pid = fork();
        if(pid < 0){
            puts("fork error");
            exit(-1);
        }else if(pid == 0){
            printf("this os chiled process, pid is %d\n", getpid());
            sleep(5);
            char s[10];
            sprintf(s,"%d",pos[i]);
            return morph(s);
        }else{
            //printf("the child pid = %d\n", pid);
            pw[i] = pid;
            //springf(pw[])
        }
    }

    for(int i=0; i< cnt; i++){
        pid_t cpid = waitpid(-1, &status, 0);
        printf("line = 137  cpid = %u  pw = %u\n", cpid, pw[i]);

        int isPrime = WEXITSTATUS(status);
        unsigned int inputNum = 0;

        for(int j = 0; j< cnt;j++){
        printf("line = 141  cpid = %u  pw = %u\n", cpid, pw[j]);
            if(cpid == pw[j]){
                inputNum = pos[j];
                break;
            }
        }

        printf("I catch a child process and this pid is %d.  input num = %u return code is %d\n", cpid, inputNum, isPrime);
    }
    free(pos);     //释放内存
	exit(0);
}


