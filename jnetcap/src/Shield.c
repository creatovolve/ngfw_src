/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

#include <jni.h>

#include <libnetcap.h>
#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <jmvutil.h>

#include "jnetcap.h"
#include JH_Shield

#define _SHIELD_OBJ_STR      JP_BUILD_NAME( Shield )
#define _SHIELD_METHOD_NAME  "callEventListener"
#define _SHIELD_METHOD_DESC  "(JDIIII)V"

static struct
{
    int       call_hook;
    jclass    class;
    jmethodID call_listener_mid;
    jobject   object;
} _shield = {
    .call_hook 0,
    .class     NULL,
    .call_listener_mid NULL,
    .object NULL
};

static void _event_hook ( in_addr_t ip, double reputation, netcap_shield_mode_t mode, 
                          int limited, int rejected, int dropped );


/*
 * Class:     com_metavize_jnetcap_Shield
 * Method:    config
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL JF_Shield( config )
    ( JNIEnv* env, jobject _this, jstring file_name )
{
    const char* file_str;
    
    if (( file_str = (*env)->GetStringUTFChars( env, file_name, NULL )) == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
    };

    debug( 5, "JNETCAP: Loading shield configuration file: %s\n", file_str );
    
    do {
        struct stat file_stat;

        if ( stat( file_str, &file_stat ) < 0 ) {
            jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "stat: %s", errstr );
            break;
        }

        if ( access( file_str, R_OK ) == 0 && S_ISREG( file_stat.st_mode )) {
            int fd;
            char buf[4096];
            int msg_len;

            /* Open and read the configuration file */
            if (( fd = open( file_str, O_RDONLY )) < 0 ) {
                jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "open: %s", errstr );
                break;
            }
            
            if (( msg_len = read( fd, buf, sizeof( buf ))) < 0 ) {
                jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "read: %s", errstr );                
                if ( close( fd ) < 0 )
                    perrlog( "close" );
                break;
            }
            
            /* Don't stop if there is an error, the data has already been read */
            if ( close( fd ) < 0 ) 
                perrlog( "close" );
            
            fd = -1;
            
            if ( msg_len == sizeof ( buf )) {
                jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "Invalid shield configuration(size>=%d)\n", 
                               sizeof ( buf ));
                continue;
            }
                        
            /* Load the shield configuration */
            if ( msg_len != 0 && netcap_shield_cfg_load ( buf, msg_len ) < 0 ) {
                jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_shield_load_configuration\n" );
                break;
            }
            
            debug( 5, "JNETCAP: Successfully loaded shield configuration\n" );
        } else {
            jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Unable to access file: '%s'", file_str );
        }
    } while ( 0 );

    (*env)->ReleaseStringUTFChars( env, file_name, file_str );
}

/*
 * Class:     com_metavize_jnetcap_Shield
 * Method:    dump
 * Signature: (LI);
 */
JNIEXPORT void JNICALL JF_Shield( status )
  (JNIEnv *env, jobject _this, jlong ip, jint port )
{
    struct sockaddr_in dst;
    int fd;
    
    memcpy( &dst.sin_addr, &ip, sizeof( struct in_addr ));
    dst.sin_port = htons((u_short)port );
    dst.sin_family = AF_INET;
    
    if (( fd = socket( AF_INET, SOCK_DGRAM, IPPROTO_UDP )) < 0 ) 
    {
        perrlog( "socket" );
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "Unable to open a UDP socket\n" );
    }

    do {
        if ( netcap_shield_status( fd, &dst ) < 0 ) {
            jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_shield_status\n" );
            break;
        }
    } while ( 0 );
    
    if ( close( fd ) < 0 )
        perrlog( "close" );
}


/*
 * Class:     com_metavize_jnetcap_Shield
 * Method:    addChunk
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL JF_Shield( addChunk )
  (JNIEnv *env, jobject _this, jlong ip, jshort protocol, jint num_bytes )
{
    /* Could throw an error, but shield errors are currently ignored. */
    if ( netcap_shield_rep_add_chunk((in_addr_t)ip, protocol, (u_short)num_bytes ) < 0 )
        errlog( ERR_WARNING, "netcap_shield_rep_add_chunk\n" );  
}

/*
 * Class:     com_metavize_jnetcap_Shield
 * Method:    registerEventListener
 * Signature: ()V
 */
JNIEXPORT void JNICALL JF_Shield( registerEventListener )
  (JNIEnv *env, jobject _this )
{
    jclass local_ref;

    /* Indicate not to call the hook until the initialization is complete */
    _shield.call_hook = 0;

    /* Get the method from the object */
    if (( local_ref = (*env)->FindClass( env, _SHIELD_OBJ_STR )) == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->FindClass\n" );
    }
    
    _shield.class = (*env)->NewGlobalRef( env, local_ref );
    
    (*env)->DeleteLocalRef( env, local_ref );
    
    if ( _shield.class == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
    }
    
    _shield.call_listener_mid = (*env)->GetMethodID( env, _shield.class, _SHIELD_METHOD_NAME, 
                                                     _SHIELD_METHOD_DESC );
    if ( _shield.call_listener_mid == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetMethodID\n" );
    }
    
    if (( _shield.object = (*env)->NewGlobalRef( env, _this )) == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
    }    
    
    if ( netcap_shield_register_hook( _event_hook ) < 0 ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_shield_register_hook\n" );
    }

    /* Indicate to call the hook now that the system is registered */
    _shield.call_hook = 1;
}

/*
 * Class:     com_metavize_jnetcap_Shield
 * Method:    removeEventListener
 * Signature: ()V
 */
JNIEXPORT void JNICALL JF_Shield( removeEventListener )
  (JNIEnv *env, jobject _this )
{
    netcap_shield_unregister_hook();

    /* Don't call the hook anymore */
    _shield.call_hook = 0;

    /* To avoid synchronization issues, the global references are never removed
     * (the shield is a singleton anyway) */
}

static void _event_hook ( in_addr_t ip, double reputation, netcap_shield_mode_t mode,
                          int limited, int rejected, int dropped )
{
    JNIEnv* env = NULL;

    if ( _shield.call_hook != 1 ) return;

    if (( env = jmvutil_get_java_env()) == NULL ) {
        errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );
        return;
    }
    
    if ( _shield.object == NULL || _shield.call_listener_mid == NULL ) {
        errlog( ERR_WARNING, "Shield hook never registered." );
        return;
    }

    /* Actually call the method */
    (*env)->CallVoidMethod( env, _shield.object, _shield.call_listener_mid, (jlong)ip, (jdouble)reputation, 
                            mode, limited, rejected, dropped );

    /* Clear out any exceptions */
    jmvutil_error_exception_clear();
}
