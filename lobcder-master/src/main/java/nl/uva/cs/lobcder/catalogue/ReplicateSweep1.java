package nl.uva.cs.lobcder.catalogue;

import nl.uva.cs.lobcder.resources.PDRIDescr;

import java.util.Collection;

/**
 * Created by dvasunin on 14.01.15.
 */
public class ReplicateSweep1 implements Runnable {
    @Override
    public void run() {

    }

  Collection<Long> selectPDRIGroupsWhichNeedToBeUpdated(){
    return null;
  }

  Collection<PDRIDescr> getPDRIDescrForGroup(Long GroupId){
    return null;
  }

  void replicate(Collection<PDRIDescr> pdriDescrs, ReplicationPolicy policy){

  }

}

/*


SELECT
  ldata_table.pdriGroupRef,
  pref_table.storageSiteRef
FROM pref_table
  JOIN ldata_table
    ON pref_table.ld_uid = ldata_table.uid
WHERE ldata_table.datatype = 'logical.file' AND pref_table.storageSiteRef NOT IN (
  SELECT storageSiteRef
  FROM pdri_table
  WHERE pdri_table.pdriGroupRef = ldata_table.pdriGroupRef
)
UNION
SELECT DISTINCT
  pdri_table.pdriGroupRef,
  NULL
FROM pdri_table
  LEFT JOIN (SELECT
               pdri_table.pdriGroupRef,
               count(pdri_table.storageSiteRef) AS refcnt
             FROM pdri_table
               JOIN storage_site_table
                 ON pdri_table.storageSiteRef = storage_site_table.storageSiteId
             WHERE NOT (storage_site_table.isCache OR storage_site_table.isRemoving)
             GROUP BY pdriGroupRef) AS t
    ON pdri_table.pdriGroupRef = t.pdriGroupRef
WHERE t.refcnt IS NULL
LIMIT 100;

select pdri_table.pdriId
from pdri_table
join (
SELECT
  pdri_table.pdriGroupRef,
  count(pdri_table.storageSiteRef) - IFNULL(refcnt1, 0)  as diff
FROM pdri_table
  JOIN storage_site_table
    ON pdri_table.storageSiteRef = storage_site_table.storageSiteId
  LEFT JOIN (
    SELECT
      pdri_table.pdriGroupRef,
      count(pdri_table.storageSiteRef) AS refcnt1
    FROM pdri_table
      JOIN storage_site_table
        ON pdri_table.storageSiteRef = storage_site_table.storageSiteId
    WHERE storage_site_table.isCache OR storage_site_table.isRemoving
    GROUP BY pdriGroupRef
    ) as t
  on pdri_table.pdriGroupRef = t.pdriGroupRef
GROUP BY pdriGroupRef
HAVING diff>0) as t2
ON pdri_table.pdriGroupRef=t2.pdriGroupRef
JOIN storage_site_table
ON pdri_table.storageSiteRef=storage_site_table.storageSiteId
where storage_site_table.isCache or storage_site_table.isRemoving;

SELECT
  pdri_table.pdriId
FROM pdri_table
JOIN (
    SELECT pdrigroup_table.pdriGroupId
    FROM pdrigroup_table
    WHERE pdriGroupId NOT IN (
      SELECT DISTINCT ldata_table.pdriGroupRef
      FROM ldata_table
        LEFT JOIN pref_table
          ON ldata_table.uid = pref_table.ld_uid
      WHERE pref_table.ld_uid IS NULL
    ) ) as t1
ON pdri_table.pdriGroupRef = t1.pdriGroupId
WHERE pdri_table.storageSiteRef not in (
  SELECT
    pref_table.storageSiteRef
  FROM pref_table
    JOIN ldata_table
    ON ldata_table.uid = pref_table.ld_uid
  where ldata_table.pdriGroupRef = t1.pdriGroupId);

 */