#include <stdlib.h>
#include <stdio.h>

int main() {
  _main();
  return 0;
}

void _malloc(int x) {
  malloc(x);
}

void _printInt(int x) {
  printf("%d\n",x);
}

void _printBool(int x) {
  printf("%s\n", (x==0 ? "false" : "true"));
}

void _printStr(char *s) {
  printf("%s\n", s);
}
