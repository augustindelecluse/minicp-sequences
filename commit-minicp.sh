#!/bin/sh

./amanda -f src/ -t ../minicp/src/
cp -r userguide ../minicp/.
cd ../minicp

message="auto-commit from $USER@$(hostname -s) on $(date)"
GIT=`which git`
${GIT} commit -a -m "$message"
gitPush=$(${GIT} push 2>&1)
echo "$gitPush"
cd ../minicp-solution
