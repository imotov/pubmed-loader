## Pubmed keywords loader

Simple loader for pubmed keywords. It loads small subset of pubmed citations into elasticsearch.  

To build, run `mvn package`.

Usage:
 * execute `mapping.sh` script in the `bin` directory to set mapping for the pubmed index (Please note 
  that this script will remove existing `pubmed` index during it's execution.
 * update `config/elasticsearch.yml` file to point to your cluster
 * execute `bin/pmloader -d path/to/pubmed/files -i path/to/impact/factor/file` 

The impact factor file is optional.