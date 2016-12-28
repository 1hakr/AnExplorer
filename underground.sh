
#!/bin/bash

# Some variables
#
ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
UNDERGROUND_DIR="$ROOT_DIR/app/src/underground"
BASE_DIR="$ROOT_DIR/app/src/main"
TEMP_DIR=`mktemp -d -t amazon`
SASH_PATH="Sash/SashOnly"

# Check if the temp directory was created successfully
#
trap "{ cd - ; rm -rf $TEMP_DIR; exit 255; }" SIGINT

cd $TEMP_DIR
echo "Temp dir: [$TEMP_DIR]"

# Download and unzip the sash from Amazon
#
echo "Download resources"
curl -s -O https://s3.amazonaws.com/amazon-underground/Sash.zip
unzip -qq Sash.zip

# Create the drawable directories (if not present)
#
echo "Prepare folders"
for i in xxxh xxh xh h tv m; do
    mkdir -p "$UNDERGROUND_DIR/res/drawable-${i}dpi"
done



# Build the overlapped images
#
echo "Build launcher icons"

for i in xxxh,192 xxh,144 xh,96 h,72 tv,64 m,48; do
    dpi=$(echo $i | cut -d',' -f 1)
    resolution=$(echo $i | cut -d',' -f 2)
    echo "...${dpi}dpi"
    composite -compose src-over \
        "${SASH_PATH}-${resolution}.png" \
        "$BASE_DIR/res/drawable-${dpi}dpi/ic_launcher.png" \
        "$UNDERGROUND_DIR/res/drawable-${dpi}dpi/ic_launcher.png"
done

# Restore the previous directory (clean script)
# and remove the temp
cd -
rm -rf $TEMP_DIR
echo "Done"
exit 0