set -e

cd `dirname $0`/..
LUANHOME=`pwd`

rm -rf build
mkdir -p build/luan/jars
cp lib/* build/luan/jars

find . -name *.class -delete

. classpath.sh

cd $LUANHOME/src
javac -classpath $CLASSPATH `find . -name *.java`
jar cvf $LUANHOME/build/luan/jars/luan.jar `find . -name *.class -o -name *.luan`

cd $LUANHOME/slf4j/src
javac -classpath $CLASSPATH `find . -name *.java`
jar cvf $LUANHOME/build/luan/jars/slf4j-goodjava.jar `find . -name *.class -o -name *.luan`

cd $LUANHOME
cp scripts/install.sh build/luan
chmod +x build/luan/install.sh
cp scripts/uninstall.sh build/luan

cd build
VERSION=`java -classpath $CLASSPATH luan.Luan classpath:luan/version.luan`
tar -cf luan-$VERSION.tar luan

luan/install.sh

echo done
