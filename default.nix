let
  pkgs = import ((import <nixpkgs> { }).fetchFromGitHub {
    owner = "NixOS";
    repo = "nixpkgs";
    rev = "b77c7e9f1d4c916e71ac271543d7ca9e5ed55862";
    sha256 = "1np9llhsls5b0y281f61sp5kih4lag06qcrjlqvqnarvx4vrgjz9";
  }) { config = { }; };
  nodejs = pkgs."nodejs-10_x";
  conan = with pkgs; import ./scripts/lib/setup/nix/conan {
    # Import a newer version of the Conan package to fix pylint issues with pinned one
    # The remaining dependencies come from Nixpkgs
    inherit lib;
    inherit python3;
    inherit git;
  };
  nodeInputs = import ./scripts/lib/setup/nix/global-node-packages/output {
    # The remaining dependencies come from Nixpkgs
    inherit pkgs;
    inherit nodejs;
  };
  nodePkgs = (map (x: nodeInputs."${x}") (builtins.attrNames nodeInputs));
in pkgs.stdenvNoCC.mkDerivation rec {
  name = "env";
  env = pkgs.buildEnv { name = name; paths = buildInputs; };
  statusDesktopBuildInputs = with pkgs; [
    cmake
    extra-cmake-modules
    go_1_10
    qt5.full # Status Desktop, cannot be installed on macOS https://github.com/NixOS/nixpkgs/issues/55892
  ] ++ stdenv.lib.optional stdenv.isLinux [conan patchelf];
  buildInputs = with pkgs; [
    gcc7
    clojure
    jq
    leiningen
    maven
    nodejs
    openjdk
    python27 # for e.g. gyp
    watchman
    unzip
    wget
    yarn
  ] ++ nodePkgs
    ++ statusDesktopBuildInputs
    ++ stdenv.lib.optional stdenv.isDarwin cocoapods;
  shellHook = with pkgs; ''
      local toolversion="$(git rev-parse --show-toplevel)/scripts/toolversion"

      export JAVA_HOME="${openjdk}"
      export ANDROID_HOME=~/.status/Android/Sdk
      export ANDROID_SDK_ROOT="$ANDROID_HOME"
      export ANDROID_NDK_ROOT="$ANDROID_SDK_ROOT/android-ndk-$($toolversion android-ndk)"
      export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
      export ANDROID_NDK="$ANDROID_NDK_ROOT"
      export PATH="$ANDROID_HOME/bin:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools:$PATH"
      export QT_PATH="${qt5.full}"

      [ -d "$ANDROID_NDK_ROOT" ] || ./scripts/setup # we assume that if the NDK dir does not exist, `make setup` needs to be run
  '';
}