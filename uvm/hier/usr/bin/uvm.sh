#! /bin/bash
# $Id$

NAME=$0

# get a bunch of default values
source @PREFIX@/etc/default/untangle-vm

UVM_CONSOLE_LOG=${UVM_CONSOLE_LOG:-"@PREFIX@/var/log/uvm/console.log"}
UVM_UVM_LOG=${UVM_UVM_LOG:-"@PREFIX@/var/log/uvm/uvm.log"}
UVM_GC_LOG=${UVM_GC_LOG:-"@PREFIX@/var/log/uvm/gc.log"}
UVM_WRAPPER_LOG=${UVM_WRAPPER_LOG:-"@PREFIX@/var/log/uvm/wrapper.log"}
UVM_LAUNCH=${UVM_LAUNCH:-"@PREFIX@/usr/share/untangle/bin/bunnicula"}

# Short enough to restart services and uvm promptly
SLEEP_TIME=15

# Used to kill a child with extreme prejudice
nukeIt() {
    echo "$NAME: Killing -9 all bunnicula \"$pid\" (`date`)" >> $UVM_WRAPPER_LOG
    kill -3 $pid
    kill -9 $pid
    kill -9 `ps awwx | grep java | grep bunnicula | awk '{print $1}'` 2>/dev/null
}

reapChildHardest() {
    nukeIt
    flushIptables ; exit
}

reapChildHarder() {
    echo "$NAME: Killing -15 all bunnicula \"$pid\" (`date`)" >> $UVM_WRAPPER_LOG
    kill $pid
    sleep 1
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "$NAME: Killing -15 all bunnicula \"$pid\" (`date`)" >> $UVM_WRAPPER_LOG
        for i in `seq 1 5` ; do
            if [ -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
                flushIptables ; exit
            fi
            sleep 1
        done
        if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
            reapChildHardest
        fi
    fi
    flushIptables ; exit
}

getVirtualMemUsage() {
  cat /proc/$1/status | awk '/VmSize/ {print $2}'
}

reapChild() {
    echo "$NAME: shutting down bunnicula " >> $UVM_WRAPPER_LOG
    kill -3 $pid
    @PREFIX@/usr/bin/mcli -t 20000 shutdown &> /dev/null
    sleep 1
    kill -INT $pid
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "$NAME: Killing -INT all bunnicula \"$pid\" (`date`)" >> $UVM_WRAPPER_LOG
        for i in `seq 1 5` ; do
            if [ -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
                flushIptables ; exit
            fi
            sleep 1
        done
        if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
            reapChildHarder
        fi
    fi
    flushIptables ; exit
}


flushIptables() {
    if [ ! -f /etc/untangle-net-alpaca/nonce ] ; then
        echo "the nonce doesn't exist, unable to regenerate rules."
        return
    fi

    local t_nonce=`head -n 1 /etc/untangle-net-alpaca/nonce`

    ## Tell the alpaca to reload the rules
    ruby <<EOF
require "xmlrpc/client"
 
client = XMLRPC::Client.new( "localhost", "/alpaca/uvm/api?argyle=${t_nonce}", 3000 )
ok, status = client.call( "generate_rules" )
EOF
}

# In 5.3, this code is only used to handle the upgrade and factory-defaults cases.
getPopId() {
  # for new installs from iso, do nothing
  [[ -f $ACTIVATION_KEY_FILE ]] || return

  # for package installs, do nothing
  [[ -f $POPID_FILE ]] && return

  # ensure we have a popid
  @UVM_HOME@/bin/utactivate
}

isServiceRunning() {
  extraArgs=""
  if [[ -n $2 ]] ; then
    extraArgs="-x"
    shift
  fi
  # If you can believe it, pidof sometimes doesn't work!
  # Calling it three times seems to be enough to work around the pidof
  # bug and "make sure" (#2534)
  let i=0
  while [[ $i -lt 3 ]] ; do
    pidof $extraArgs "$1" && return 0
    let i=$i+1
    sleep .5
  done
  return 1
}

restartServiceIfNeeded() {
  serviceName=$1

  case $serviceName in
    postgresql)
# Removing the postgres pid file just makes restarting harder.  The init.d script deals ok as is.
      pidFile=/tmp/foo-doesnt-exist
      isServiceRunning $PG_DAEMON_NAME && return
      serviceName=$PGSERVICE
      ;;
    mongrel)
      pidFile=/tmp/foo-doesnt-exist
      isServiceRunning --find-shell mongrel_rails && return
      serviceName="untangle-net-alpaca untangle-net-alpaca-iptables"
      ;;
    slapd)
      pidFile=/var/run/untangle-ldap/slapd.pid
      dpkg -l untangle-ldap-server | grep -q -E '^ii' || return
      isServiceRunning slapd && return
      ;;
    snmpd)
      pidFile=/var/run/snmpd.pid
      dpkg -l snmpd | grep -q -E '^ii' || return
      isServiceRunning snmpd && return
      ;;
    spamassassin)
      pidFile=$SPAMASSASSIN_PID_FILE
      dpkg -l untangle-spamassassin-update | grep -q -E '^ii' || return
      isServiceRunning --find-shell spamd && return
      ;;
    clamav-daemon)
      pidFile=$CLAMD_PID_FILE
      dpkg -l untangle-clamav-config | grep -q -E '^ii' || return
      isServiceRunning clamd && return
      ;;
    clamav-freshclam)
      pidFile="/var/run/clamav/freshclam.pid"
      dpkg -l untangle-clamav-config | grep -q -E '^ii' || return
      isServiceRunning freshclam && return
      ;;
    untangle-support-agent)
      pidFile="/var/run/rbot.pid"
      dpkg -l untangle-support-agent | grep -q -E '^ii' || return
      # this is a bit janky, need something better...
      isServiceRunning ruby && return
      ;;
    kav)
      pidFile="/var/run/aveserver.pid"
      dpkg -l untangle-kav | grep -q -E '^ii' || return
      isServiceRunning aveserver && return
      ;;
  esac

  for service in $serviceName ; do
    restartService $service $pidFile "missing"
  done
}

restartService() {
  serviceName=$1
  pidFile=$2
  reason=$3
  stopFirst=$4
  hook=$5
  servicePid=
  echo "*** restarting $reason $serviceName on `date` ***" >> $UVM_WRAPPER_LOG
  if [ -n "$stopFirst" ] ; then
    # netstat -plunt >> $UVM_WRAPPER_LOG
    [ -n "$pidFile" ] && servicePid=`cat $pidFile`
    /etc/init.d/$serviceName stop
    # just to be sure
    [ -n "$servicePid" ] && kill -9 $servicePid
  else # remove the pidfile
    [ -n "$pidFile" ] && rm -f $pidFile
  fi
  eval "$5"
  /etc/init.d/$serviceName start
}

# Return true (0) when we need to reap and restart the uvm.
needToRestart() {
    cheaphigh=`head -3 /proc/$pid/maps | tail -1 | awk '{ high=split($1, arr, "-"); print arr[2]; }'`
    if [ -z $cheaphigh ]; then
        # not fatal, process has probably just died, which we'll catch soon.
        echo "*** no heap size ($cheaphigh) on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
    else
        bignibble=${cheaphigh:0:1}
        case $bignibble in
            0 | 1)
                # less than 384Meg native heap
                ;;
            2)
                # 384Meg < native heap < 640Meg
                if [ $MEM -lt 1000000 ] || [ `date +%H` -eq 1 ] ; then
                    echo "*** bunnicula heap soft limit on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
                    return 0;
                fi
                ;;
            3 | 4 | 5 | 6 | 7 | 8 | 9)
                # native heap > 640Meg
                echo "*** bunnicula heap hard limit ($bignibble) on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
                return 0;
                ;;
            *)
                echo "*** unexpected heap size ($bignibble) on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
                ;;
        esac
    fi

    #  garbage collection failure (usually happens when persistent heap full)
    cmfcount=`tail -50 $UVM_GC_LOG | grep -ci "concurrent mode failure"`
    if [ $cmfcount -gt 2 ]; then
        echo "*** java heap cmf on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
        return 0;
    fi

    # extra nightime checks
    if [ `date +%H` -eq 1 ]; then
        # VSZ greater than 1.1 gigs reboot
        VIRT="`getVirtualMemUsage $pid`"
        if [ $VIRT -gt $MAX_VIRTUAL_SIZE ] ; then
            echo "*** Virt Size too high ($VIRT) on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
            return 0;
        fi
    fi

    return 1;
}


trap reapChildHardest 6
trap reapChildHarder 15
trap reapChild 2

while true; do
    echo > $UVM_CONSOLE_LOG
    echo "============================" >> $UVM_CONSOLE_LOG
    echo $UVM_LAUNCH >> $UVM_CONSOLE_LOG
    echo "============================" >> $UVM_CONSOLE_LOG

    echo >> $UVM_WRAPPER_LOG
    echo "============================" >> $UVM_WRAPPER_LOG
    echo $UVM_LAUNCH >> $UVM_WRAPPER_LOG
    echo "============================" >> $UVM_WRAPPER_LOG

    flushIptables

    getPopId

    $UVM_LAUNCH $* >>$UVM_CONSOLE_LOG 2>&1 &

    pid=$!
    echo "Bunnicula launched. (pid:$pid) (`date`)" >> $UVM_WRAPPER_LOG

# Instead of waiting, we now monitor.

    counter=0

    while true; do

        sleep $SLEEP_TIME
	let counter=${counter}+$SLEEP_TIME

        if [ "x" = "x@PREFIX@" ] ; then
            if [ ! -d /proc/$pid ] ; then
                echo "*** restarting missing bunnicula $? on `date` ***" >> $UVM_WRAPPER_LOG
                break
            fi
            if needToRestart; then
                echo "*** need to restart bunnicula $? on `date` ***" >> $UVM_WRAPPER_LOG
                nukeIt
                break
            fi
            restartServiceIfNeeded postgresql
            restartServiceIfNeeded clamav-freshclam
            restartServiceIfNeeded clamav-daemon
            restartServiceIfNeeded spamassassin
            restartServiceIfNeeded slapd
            restartServiceIfNeeded untangle-support-agent
            restartServiceIfNeeded mongrel
            restartServiceIfNeeded kav
	    
	    if [ $counter -gt 60 ] ; then # fire up the other nannies
                curl -sf -m 10 "http://localhost:3000/alpaca/dns?argyle=`head -n 1 /etc/untangle-net-alpaca/nonce`" > /dev/null
	        rc=$?
	        if [ $rc -ne 0 ] ; then
		  restartService untangle-net-alpaca /tmp/foo-doesnt-exist "non-functional (RC=$rc)"
		  restartService untangle-net-alpaca-iptables /tmp/foo-doesnt-exist "non-functional (RC=$rc)"
		fi

	        if dpkg -l untangle-spamassassin-update | grep -q ii ; then # we're managing spamassassin
	            [ `tail -n 50 /var/log/mail.info | grep -c "$SPAMASSASSIN_LOG_ERROR"` -gt 2 ] && restartService spamassassin $SPAMASSASSIN_PID_FILE "non-functional" stopFirst
                    case "`$BANNER_NANNY $SPAMASSASSIN_PORT $TIMEOUT`" in
                      *success*) true ;;
                      *) restartService spamassassin $SPAMASSASSIN_PID_FILE "hung" stopFirst ;;
                    esac
                fi
	        if dpkg -l untangle-clamav-config | grep -q -E '^ii' ; then # we're managing clamav
	          [ `tail -n 50 /var/log/clamav/clamav.log  | grep -c -E "$CLAMAV_LOG_ERROR"` -gt 2 ] && restartService clamav-daemon $CLAMD_PID_FILE "non-functional" stopFirst '/etc/init.d/clamav-freshclam stop ; rm -fr /var/lib/clamav/* ; freshclam ; /etc/init.d/clamav-freshclam restart'
                  case "`$BANNER_NANNY $CLAMD_PORT $TIMEOUT`" in
                    *success*) true ;;
                    *) restartService clamav-daemon $CLAMD_PID_FILE "hung" stopFirst ;;
                  esac
		  # memory-management
		  VIRT="`getVirtualMemUsage $(cat $CLAMD_PID_FILE)`"
		  if [ $VIRT -gt $CLAMD_MAX_VIRTUAL_SIZE ] ; then
		    restartService clamav-daemon $CLAMD_PID_FILE "memory-hogging ($VIRT)" stopFirst
		  fi
	        fi
	        if dpkg -l untangle-support-agent | grep -q ii ; then # support-agent is supposed to run
	            if [ -f "$SUPPORT_AGENT_PID_FILE" ] && ps `cat $SUPPORT_AGENT_PID_FILE` > /dev/null ; then # it runs
	                if [ $(ps -o %cpu= `cat $SUPPORT_AGENT_PID_FILE` | perl -pe 's/\..*//') -gt $SUPPORT_AGENT_MAX_ALLOWED_CPU ] ; then
		            restartService untangle-support-agent $SUPPORT_AGENT_PID_FILE "spinning" stopFirst
	                fi
	            fi
	        fi
	        counter=0
	    fi

        fi

    done

# Clean up the zombie.  Risky? XXX
#    wait $pid

# Crash/Kill
    flushIptables
    echo "*** bunnicula exited on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
    echo "*** copied $UVM_CONSOLE_LOG to $UVM_CONSOLE_LOG.crash ***" >> $UVM_WRAPPER_LOG
    echo "*** copied $UVM_UVM_LOG to $UVM_UVM_LOG.crash ***" >> $UVM_WRAPPER_LOG
    echo "*** copied $UVM_GC_LOG to $UVM_GC_LOG.crash ***" >> $UVM_WRAPPER_LOG
    cp -fa $UVM_CONSOLE_LOG.crash.1 $UVM_CONSOLE_LOG.crash.2
    cp -fa $UVM_CONSOLE_LOG.crash $UVM_CONSOLE_LOG.crash.1
    cp -fa $UVM_CONSOLE_LOG $UVM_CONSOLE_LOG.crash
    cp -fa $UVM_UVM_LOG.crash.1 $UVM_UVM_LOG.crash.2
    cp -fa $UVM_UVM_LOG.crash $UVM_UVM_LOG.crash.1
    cp -fa $UVM_UVM_LOG $UVM_UVM_LOG.crash
    cp -fa $UVM_GC_LOG.crash.1 $UVM_GC_LOG.crash.2
    cp -fa $UVM_GC_LOG.crash $UVM_GC_LOG.crash.1
    cp -fa $UVM_GC_LOG $UVM_GC_LOG.crash

    sleep 2
    echo "*** restarting on `date` ***" >> $UVM_WRAPPER_LOG
done
