#! /bin/zsh
set -o shwordsplit

if [ "$ICSCROWDAPIKEY" != "" ]
then
	echo "Generating new translation archives"
	fetch -q -1 -o - "http://api.crowdin.net/api/project/ics-openvpn/export?key=$ICSCROWDAPIKEY"
fi

echo "Fetch translation archive"
fetch -q http://crowdin.net/download/project/ics-openvpn.zip


# Chinese language require zh-CN and zh-TW

typeset -A langhash
langhash=(zh-CN zh-rCN zh-TW zh-rTW id-ID in ca-ES ca cs-CZ cs et-EE et ja-JP ja ko-KR ko sv-SE sv uk-UA uk)

langtoinclude="de es fr hu it no nl pl pt ro ru tr"

for lang in $langtoinclude ${(k)langhash}
do
    if (( ${+langhash[$lang]} )); then
        alang=$lang
        rlang=${langhash[$lang]}
    else
        alang=$lang-${lang:u}
        rlang=$lang
    fi

    tar -xv -C src/main/res/values-$rlang/ --strip-components 3 -f ics-openvpn.zip res/values-$alang/
done

rm ics-openvpn.zip
