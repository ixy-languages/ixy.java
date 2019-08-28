#!/usr/bin/env bash

# Clone, fix and compile MoonGen
git clone https://github.com/emmericp/Moongen
cd Moongen
git submodule update --init --recursive
sed -i 's/^git submodule update/#git submodule update/' build.sh
cd libmoon
sed -i 's/^git submodule update/#git submodule update/' build.sh
find . -type f | xargs sed -i 's/-Werror//g'
cd ../..
bash MoonGen/build.sh

# Clone and compile ixy
git clone https://github.com/emmericp/ixy
cd ixy
cmake .
make
cd ..

# Clone and compile the benchmark script for MoonGen
git clone https://github.com/ixy-languages/benchmark-scripts
