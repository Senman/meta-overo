DESCRIPTION = "A TCP/IP Daemon simplifying the communication with GPS devices"
SECTION = "console/network"
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://COPYING;md5=d217a23f408e91c94359447735bc1800"
DEPENDS = "dbus dbus-glib ncurses python libusb1 chrpath-replacement-native"
PROVIDES = "virtual/gpsd"

EXTRANATIVEPATH += "chrpath-native"

PR = "r0"

#    file://0002-SConstruct-respect-sysroot-also-in-SPLINTOPTS.patch
#    file://0004-SConstruct-remove-rpath.patch
#    file://0001-SConstruct-prefix-includepy-with-sysroot-and-drop-sy.patch
#    file://0001-SConstruct-disable-html-and-man-docs-building-becaus.patch

SRC_URI = "http://download.savannah.gnu.org/releases/${PN}/${P}.tar.gz \
    file://0001-SConstruct-prefix-includepy-with-sysroot-and-drop-sy.patch \
    file://0004-SConstruct-remove-rpath.patch \
    file://gpsd-default \
    file://gpsd \
    file://60-gpsd.rules \
"

SRC_URI[md5sum] = "fc5b03aae38b9b5b6880b31924d0ace3"
SRC_URI[sha256sum] = "706fc2c1cf3dfbf87c941f543381bccc9c4dc9f8240eec407dcbf2f70b854320"

#inherit scons update-rc.d python-dir pythonnative systemd
inherit scons update-rc.d python-dir pythonnative

INITSCRIPT_NAME = "gpsd"
INITSCRIPT_PARAMS = "defaults 98"

# SYSTEMD_OESCONS = "${@base_contains('DISTRO_FEATURES', 'systemd', 'true', 'false',d)}"
SYSTEMD_OESCONS = "false"

export STAGING_INCDIR
export STAGING_LIBDIR

EXTRA_OESCONS = " \
    sysroot=${STAGING_DIR_TARGET} \
    libQgpsmm='false' \
    debug='true' \
    strip='false' \
    systemd='${SYSTEMD_OESCONS}' \
"
# this cannot be used, because then chrpath is not found and only static lib is built
# target=${HOST_SYS}

do_compile_prepend() {
    export PKG_CONFIG_PATH="${PKG_CONFIG_PATH}"
    export PKG_CONFIG="PKG_CONFIG_SYSROOT_DIR=\"${PKG_CONFIG_SYSROOT_DIR}\" pkg-config"
    export STAGING_PREFIX="${STAGING_DIR_HOST}/${prefix}"

    export BUILD_SYS="${BUILD_SYS}"
    export HOST_SYS="${HOST_SYS}"
}

do_install() {
    export PKG_CONFIG_PATH="${PKG_CONFIG_PATH}"
    export PKG_CONFIG="PKG_CONFIG_SYSROOT_DIR=\"${PKG_CONFIG_SYSROOT_DIR}\" pkg-config"
    export STAGING_PREFIX="${STAGING_DIR_HOST}/${prefix}"

    export BUILD_SYS="${BUILD_SYS}"
    export HOST_SYS="${HOST_SYS}"

    export DESTDIR="${D}"
    # prefix is used for RPATH and DESTDIR/prefix for instalation
    ${STAGING_BINDIR_NATIVE}/scons prefix=${prefix} install ${EXTRA_OESCONS}|| \
      bbfatal "scons install execution failed."
}

do_install_append() {
    install -d ${D}/${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/gpsd ${D}/${sysconfdir}/init.d/
    install -d ${D}/${sysconfdir}/default
    install -m 0644 ${WORKDIR}/gpsd-default ${D}/${sysconfdir}/default/gpsd.default

    #support for udev
    install -d ${D}/${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/60-gpsd.rules ${D}/${sysconfdir}/udev/rules.d
    install -d ${D}${base_libdir}/udev/
    install -m 0755 ${S}/gpsd.hotplug ${D}${base_libdir}/udev/

    #support for python
    install -d ${D}/${PYTHON_SITEPACKAGES_DIR}/gps
    install -m 755 ${S}/gps/*.py ${D}/${PYTHON_SITEPACKAGES_DIR}/gps

    #support for systemd
#    install -d ${D}${systemd_unitdir}/system/
#    install -m 0644 ${WORKDIR}/${PN}.service ${D}${systemd_unitdir}/system/${PN}.service
#    install -m 0644 ${S}/systemd/${PN}.socket ${D}${systemd_unitdir}/system/${PN}.socket
}

pkg_postinst_${PN}-conf() {
    update-alternatives --install ${sysconfdir}/default/gpsd gpsd-defaults ${sysconfdir}/default/gpsd.default 10
}

pkg_postrm_${PN}-conf() {
    update-alternatives --remove gpsd-defaults ${sysconfdir}/default/gpsd.default
}

PACKAGES =+ "libgps libgpsd python-pygps-dbg python-pygps gpsd-udev gpsd-conf gpsd-gpsctl gps-cgps gps-utils"

FILES_${PN}-dev += "${libdir}/pkgconfdir/libgpsd.pc ${libdir}/pkgconfdir/libgps.pc"

FILES_python-pygps-dbg += " ${libdir}/python*/site-packages/gps/.debug"

RDEPENDS_${PN} = "gpsd-gpsctl"
RRECOMMENDS_${PN} = "gpsd-conf gpsd-udev gpsd-machine-conf"

DESCRIPTION_gpsd-udev = "udev relevant files to use gpsd hotplugging"
FILES_gpsd-udev = "${base_libdir}/udev ${sysconfdir}/udev/*"
RDEPENDS_gpsd-udev += "udev gpsd-conf"

DESCRIPTION_libgpsd = "C service library used for communicating with gpsd"
FILES_libgpsd = "${libdir}/libgpsd.so.*"

DESCRIPTION_libgps = "C service library used for communicating with gpsd"
FILES_libgps = "${libdir}/libgps.so.*"

DESCRIPTION_gpsd-conf = "gpsd configuration files and init scripts"
FILES_gpsd-conf = "${sysconfdir}"

DESCRIPTION_gpsd-gpsctl = "Tool for tweaking GPS modes"
FILES_gpsd-gpsctl = "${bindir}/gpsctl"

DESCRIPTION_gps-cgps = "Tool for testing GPS"
FILES_gps-cgps = "${bindir}/cgps"

DESCRIPTION_gps-utils = "Utils used for simulating, monitoring,... a GPS"
FILES_gps-utils = "${bindir}/*"
RDEPENDS_gps-utils = "python-pygps"

DESCRIPTION_python-pygps = "Python bindings to gpsd"
FILES_python-pygps = "${PYTHON_SITEPACKAGES_DIR}/*"
RDEPENDS_python-pygps = "python-core python-curses gpsd python-json"

#RPROVIDES_${PN} += "${PN}-systemd"
#RREPLACES_${PN} += "${PN}-systemd"
#RCONFLICTS_${PN} += "${PN}-systemd"
#SYSTEMD_SERVICE_${PN} = "${PN}.socket"

