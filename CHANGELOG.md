# Changelog

## [1.25.0](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/compare/v1.24.0...v1.25.0) (2025-01-16)


### Features

* Support for CREATE SCHEMA statements ([#197](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/197)) ([e19264c](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/e19264c2b66d92295cbea044e7772084b969d3d7))


### Bug Fixes

* **deps:** update mvn-packages ([#193](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/193)) ([1c9cc6f](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/1c9cc6f2bd887837da47cc1676e7dfa6b5e07d7d))

## [1.24.0](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/compare/v1.23.0...v1.24.0) (2024-12-04)


### Features

* expose isStored on column ([#177](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/177)) ([fe7fc2a](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/fe7fc2a1ffa526c9f587270b5ef85784be3d7b57))


### Bug Fixes

* **deps:** update error-prone to 2.36 ([de62eea](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/de62eeabd5be80082dabcbe9685b92a41c5268da))
* **deps:** update mvn-packages ([#192](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/192)) ([3ee6d2d](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/3ee6d2dec7e3264519a34c50a7383d46bb46f8b1))
* do not capitalize reserved words for object names ([#191](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/191)) ([da7da8e](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/da7da8e9f79ebaf9801e216548b6add4383eeb4c))
* equals ASTcheck_constraint ([#181](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/181)) ([fec08aa](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/fec08aa929524a0f5b70672cd317b17bc7dcf646))

## [1.23.0](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/compare/v1.22.3...v1.23.0) (2024-10-05)


### Features

* made some methods of DdlDiff and DatabaseDefinition public so that it can be invoked within a Java application. ([#158](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/158)) ([af1db7d](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/af1db7dd4ee2ba962604c5cea1b0e8f73dc04d1f))
* make Spanner SQL DDL parsing public ([#169](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/169)) ([b1b237b](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/b1b237b32661a269f43d211baf5252c8362aa9d4))
* support annotations in SQL file ([#171](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/171)) ([63efbd6](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/63efbd654ebeccecf15ec9f9ed6c9b041f296803))


### Bug Fixes

* delta generation for SEARCH INDEX as the ASC keyword is not supported ([#174](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/174)) ([897d4b0](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/897d4b05030ccd41ae51c8cff377e9d70696b0e4))
* **deps:** bump com.google.errorprone:error_prone_core ([#147](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/147)) ([170ac33](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/170ac3375a94c09a6a8788ec7f24eeacbb89d78b))
* **deps:** bump com.google.guava:guava from 33.3.0-jre to 33.3.1-jre ([#172](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/172)) ([c367a81](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/c367a81502caffd893c05763945e61e984d24349))
* **deps:** bump slf4j.version from 2.0.13 to 2.0.16 ([#151](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/151)) ([07a1bcf](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/07a1bcf15519ebfce0eb9a2fd3147874cff44d50))
* **deps:** update dependency com.google.guava:guava to v33.3.0-jre ([#160](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/160)) ([3afddc1](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/3afddc10e5fd14f6a88dc805a7877dba8ab9e892))
* **deps:** update dependency com.google.guava:guava to v33.3.1-jre ([#170](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/170)) ([b35161a](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/b35161a35fa6c7983c596067cb5ac7eb4472efcb))
* **deps:** update dependency commons-cli:commons-cli to v1.9.0 ([#159](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/159)) ([ada63bb](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/ada63bbfd0e17d810de12cc207d2d8b790a81be6))
* null options for equivalent change streams ([#163](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/163)) ([331b0fe](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/331b0fe79a315acf5ddfd52faf9c11a6920d5b0f))

## [1.22.3](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/compare/v1.22.2...v1.22.3) (2024-06-19)


### Bug Fixes

* add equals to ASToptions_clause ([#138](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/138)) ([1fddeea](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/1fddeeab8b921eb4de73788022288886c6dd39fe)), closes [#117](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/117)

## [1.22.2](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/compare/v1.22.1...v1.22.2) (2024-06-19)


### Bug Fixes

* **deps:** update dependency com.google.guava:guava to v33.2.1-jre ([#124](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/124)) ([4da2a06](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/4da2a06a0bd458ac725a3faf3974e9a9294f016f))
* handle cases where there are no stored columns in search indexes ([#136](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/136)) ([caf211a](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/caf211ac4e869567315da7b9472f5692a316f977)), closes [#118](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/118)

## [1.22.1](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/compare/v1.22.0...v1.22.1) (2024-05-27)


### Bug Fixes

* correct config of maven assembly plugin ([#116](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/116)) ([380799b](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/380799bb65c7e9f299a3570f831eeb31d01f09eb)), closes [#109](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/109)
* create_search_index_statement equals ([#114](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/114)) ([6ea341e](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/6ea341e0ad24448048835dc0fc0548be811796c2)), closes [#111](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/111)
* **deps:** bump commons-cli:commons-cli from 1.7.0 to 1.8.0 ([#112](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/112)) ([c9ec7a1](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/c9ec7a1fb2f5be59f72c09cb3f8c0dedafaca7ed))
* **deps:** bump org.codehaus.mojo:build-helper-maven-plugin ([#110](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/110)) ([0fbc5db](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/0fbc5db72812bf462c1541545aba893d98b58b61))
* **deps:** bump org.codehaus.mojo:exec-maven-plugin from 3.2.0 to 3.3.0 ([#113](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/113)) ([f63a7c8](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/f63a7c846fa009c247d814e5d3a85d94edc60ebb))

## [1.22.0](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/compare/v1.21.0...v1.22.0) (2024-05-14)


### Features

* Add support for FLOAT32 column type ([e5ee7c1](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/e5ee7c15f5e0b07ad03ea73e3edf1c598d956e75))
* Support for diffs on Create Search Index ([#104](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/104)) ([5a567ed](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/5a567ed6abf3f3ba5e0beb33a50d793162c06f28))


### Bug Fixes

* **deps:** bump com.google.errorprone:error_prone_core ([#108](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/108)) ([4a4c0ad](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/4a4c0adabc9f0f5994bfa71f8fe3a233fd563f5f))
* **deps:** bump com.google.guava:guava from 33.1.0-jre to 33.2.0-jre ([#107](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/107)) ([548e2cb](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/548e2cb5552f4a7f0d7a1ec00d8493dd972c0347))

## [1.21.0](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/compare/v1.20.0...v1.21.0) (2024-05-01)


### Features

* Update parser to handle RENAME and SYNONYM ([#101](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/101)) ([49a08fd](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/49a08fda9f2359027256c345708c6e7819cc7607))


### Bug Fixes

* Compilation issues with error-prone on JDK16+ ([#100](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/100)) ([7e98200](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/7e9820066cd2478ac73a348483c952c99780b3c3))
* **deps:** bump com.google.errorprone:error_prone_core ([#99](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/99)) ([b51fa4c](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/b51fa4c613e95049b32ca63233d1438abe4ab030))
* **deps:** Bump com.google.errorprone:error_prone_core from 2.25.0 to 2.26.1 ([#93](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/93)) ([1c1cb59](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/1c1cb59b489391e82b764f7a29c519a9c492cde3))
* **deps:** Bump com.google.guava:guava from 33.0.0-jre to 33.1.0-jre ([#92](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/92)) ([1dd2837](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/1dd2837b69534fc4d94af8cdafac2ca53035563d))
* **deps:** bump commons-cli:commons-cli from 1.6.0 to 1.7.0 ([#98](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/98)) ([e9a3514](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/e9a35148362fbf380909f3e69d750185809b2c92))
* **deps:** Bump org.apache.maven.plugins:maven-compiler-plugin ([#95](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/95)) ([226e5a2](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/226e5a2880a736339b9080690190bbfed8daa4da))
* **deps:** Bump slf4j.version from 2.0.12 to 2.0.13 ([#96](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/96)) ([e4b0471](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/e4b047185df82452a999c32e62d135084fa75b0c))
* Update dependabot.yml to add conventional commit prefix ([f0a4cb7](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/f0a4cb77f81ece7cef76cdc4fc3b8c85115c76d8))
* Update dependabot.yml to add conventional commit prefix ([97243fa](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/97243fa3952f344231ad30dff15ba14af8dc40b3))

## 1.20.0 (2024-03-11)


### Features

* Add handling for updating stored columns on indexes ([eea1b22](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/eea1b222f1532b942c2b31b1377d4eba090e5c86))


### Bug Fixes

* library should released as 1.20.0 ([#91](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/issues/91)) ([efd280c](https://github.com/cloudspannerecosystem/spanner-schema-diff-tool/commit/efd280cc64d217443d8a19660aec8677339da65e))
