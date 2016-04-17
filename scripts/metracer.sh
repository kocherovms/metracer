#!/bin/sh

usage() {
${usage_placeholder}
}

if [ $# -eq 0 ]; then
	usage
	exit 1
fi

export METRACER_LAUNCH_STRING="metracer.sh"
# Thanks to https://coderwall.com/p/ssuaxa/how-to-make-a-jar-file-linux-executable
MYSELF=$(which "$0" 2>/dev/null)
[ $? -gt 0 -a -f "$0" ] && MYSELF="$0"

if test -n "$JAVA_HOME"; then
	java="$JAVA_HOME/bin/java"
else
	java=$(which java 2>/dev/null)
fi

if [ ! -f "$java" ]; then
	printf "Failed to find 'java' executable. Please, make sure that Java is installed\n" 1>&2
	exit 1
fi

is_help_requested=""
is_list_requested=""

while getopts ":vhlr" opt; do
	case $opt in
		h) 
			is_help_requested="1"
			;;
		l) 
			is_list_requested="1"
			;;
		r) 
			;;
		v) 
			;;
		\?)
			printf "Unknown option -$OPTARG\n" 1>&2
			usage
			exit 1
			;;
	esac
done

if [ -n "$is_help_requested" ]; then
	exec "$java" -jar $MYSELF "$@"
	exit 1 # Must never get here (exec is used)
fi

# For all other options we need tools.jar, resolve it
if test -n "$JAVA_HOME"; then
	tools_jar=$JAVA_HOME/lib/tools.jar
else
	java=$(readlink -f "$java")
	tools_jar=$(dirname "$java")/../lib/tools.jar

	if [ ! -f "$tools_jar" ]; then
		# Case when we are in a jre subdirectory of a JDK
		tools_jar=$(dirname "$java")/../../lib/tools.jar
	fi
fi

if [ ! -f "$tools_jar" ]; then
	printf "Failed to resolve tools.jar from a JDK. Please, make sure that JDK is installed and JAVA_HOME environment variable is properly set\n" 1>&2
	exit 1
fi

if [ -n "$is_list_requested" ]; then
	# For JVM listing no impersonation is required	
	"$java" -Xbootclasspath/a:"$tools_jar" -jar $MYSELF "$@"
    RV=$?

	if [ "$(id -u)" != "0" ]; then
	    printf '\nWARNING: JVM list may be not full (missing root privileges). Use `sudo sh %s -l` to get a full list\n' "$0" 1>&2
	fi

	exit $RV
fi

eval pid="\$$OPTIND"
user_name=""

if [ -n "$pid" ]; then
	user_name=$(ps -Ao pid,user | grep -P "^\s*$pid" | head -n1 | awk '{ print $2 }')
fi

current_user_name=$(whoami)
stty_save=$(stty -g)
stty -echo
stty cbreak
export METRACER_IS_CBREAK_DISABLED="1"

if [ -n "$user_name" ] && [ "$current_user_name" != "$user_name" ]; then
	sudo -u $user_name METRACER_LAUNCH_STRING=$METRACER_LAUNCH_STRING METRACER_IS_CBREAK_DISABLED=$METRACER_IS_CBREAK_DISABLED "$java" -Xbootclasspath/a:"$tools_jar" -jar $MYSELF "$@"
	RV=$?
else
	"$java" -Xbootclasspath/a:"$tools_jar" -jar $MYSELF "$@"
	RV=$?
fi

stty $stty_save
exit $RV
