java -jar eva-pipeline/target/eva-pipeline-0.1.jar \
 --spring.batch.job.names=variantJob \
 input=data/small.vcf \
 fileId=5 \
 aggregated=NONE \
 studyType=COLLECTION \
 studyName=studyName \
 studyId=7 \
 outputDir= \
 pedigree= \
 dbName=batch \
 storageEngine=mongodb \
 overwriteStats=true \
 compressGenotypes=true \
 compressExtension=.gz \
 includeSrc=FIRST_8_COLUMNS \
 vepInput=out/variants.preannot.tsv.gz \
 vepPath="/nfs/production2/eva/VEP/ensembl-tools-release-78/scripts/variant_effect_predictor/variant_effect_predictor.pl" \
 vepParameters="--force_overwrite --cache --cache_version 78 -dir /nfs/production2/eva/VEP/cache_1 --offline -o STDOUT --species homo_sapiens --everything" \
 vepFasta="--fasta /nfs/production2/eva/VEP/cache_1/homo_sapiens/78_GRCh37/Homo_sapiens.GRCh37.75.dna.primary_assembly.fa" \
 vepOutput=out/variants.annot.tsv.gz \
 skipAnnotCreate=true \
 skipLoad=false \
 opencga.app.home=/opt/opencga

