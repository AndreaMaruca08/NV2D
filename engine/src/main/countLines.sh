#!/bin/bash

find . -type f -name "*.java" -print0 |
while IFS= read -r -d '' file; do
    lines=$(wc -l < "$file")
    printf "%s\t%s\n" "$file" "$lines"
done |
sort -n -k2 |
awk -F '\t' '
{
    files[NR] = $1
    lines[NR] = $2

    total += $2

    if (length($1) > maxFile)
        maxFile = length($1)

    if ($2 > maxLines)
        maxLines = $2
}
END {
    widthFile = maxFile + 1
    widthLines = length(maxLines)

    for (i = 1; i <= NR; i++)
        printf "%-*s -> %*d\n", widthFile, files[i], widthLines, lines[i]

    printf "\n%-*s -> %*d\n", widthFile, "TOTALE", widthLines, total
}'