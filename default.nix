with import (builtins.fetchTarball {
  # Descriptive name to make the store path easier to identify
  name = "nixos-unstable-2019-02-12";
  # Commit hash for nixos-unstable as of 2019-02-12 (obtained at https://howoldis.herokuapp.com/ from channel 'nixos-18.09')
  url = https://github.com/nixos/nixpkgs/archive/168cbb39691cca2822ce1fdb3e8c0183af5c6d0d.tar.gz;
  # Hash obtained using `nix-prefetch-url --unpack <url>`
  sha256 = "0fqasswfqrz2rbag9bz17j8y7615s0p9l23cw4sk2f384gk0zf6c";
}) {};
stdenv.mkDerivation rec {
  name = "env";
  env = buildEnv { name = name; paths = buildInputs; };
  buildInputs = [
    cmake
    extra-cmake-modules
    clojure
    go_1_10
    leiningen
    maven
    nodejs-10_x
    openjdk
    python27 # for e.g. gyp
    # qt511.full # does not install on macOS due to incompatible dependency `bluez`
    # Nix only has versions 5.11.1 and 5.11.3, not the 5.11.2 we currently use
    # qt5.qtbase
    # qt5.qtdeclarative
    # qt5.qtgraphicaleffects
    # qt5.qtimageformats
    # qt5.qtlocation
    # qt5.qtquickcontrols
    # qt5.qtquickcontrols2
    # qt5.qtsensors
    # qt5.qtserialport
    # qt5.qtsvg
    # qt5.qttools
    # qt5.qtwebchannel
    # qt5.qttranslations
    watchman
    unzip
    wget
    yarn
  # ] ++ stdenv.lib.optional stdenv.isLinux conan; # Causing build errors in pylint when fetching 168cbb39691cca2822ce1fdb3e8c0183af5c6d0d, supposedly fixed in https://github.com/NixOS/nixpkgs/issues/51394
  ] ++ stdenv.lib.optional stdenv.isLinux python37; # for Conan
  shellHook = with pkgs; ''
      local toolversion="$(git rev-parse --show-toplevel)/scripts/toolversion"

      export JAVA_HOME="${openjdk}"
      export ANDROID_HOME=~/.status/Android/Sdk
      export ANDROID_SDK_ROOT="$ANDROID_HOME"
      export ANDROID_NDK_ROOT="$ANDROID_SDK_ROOT/android-ndk-$($toolversion android-ndk)"
      export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
      export ANDROID_NDK="$ANDROID_NDK_ROOT"
      export PATH="$ANDROID_HOME/bin:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools:$PATH"

      [ -d "$ANDROID_NDK_ROOT" ] || ./scripts/setup # we assume that if the NDK dir does not exist, `make setup` needs to be run
  '';
}