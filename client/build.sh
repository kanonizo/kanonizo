
if [ ! -d "scythe" ]; then
  git clone --depth=1 https://github.com/thomasdeanwhite/scythe.git
fi
pushd scythe > /dev/null 2>&1
  mvn clean package -Dmaven.clean.failOnError=false
popd

mvn clean package install -Dmaven.clean.failOnError=false


