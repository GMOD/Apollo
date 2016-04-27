<?php

// function to loop menu array
function loop_array($db, $array = array(), $parent_id = 0) {
  if(!empty($array[$parent_id])) {
     echo '<ul>';
     foreach($array[$parent_id] as $items) {
	   if($items->has_children == 1)  
         echo '<li class="has-sub">';
	   else
		 echo '<li>';
	 
       $path =  $items->link_path;	  
	   if(!empty($path)) {
         $path_sql = pg_query($db, "select * from url_alias where source='".$path."'"); 	   
	     $path_res = pg_fetch_object($path_sql); 
		 
	     if(!empty($path_res->alias)) 
		   $path = $path_res->alias;   	
	   }	 
	   
	   if($items->link_path == '<front>') 
	     $path = ''; 
	 
	   $current_path = 'http://gmod-dev.nal.usda.gov/'; 
       echo "<a href='".$current_path.$path."'>".$items->link_title;	   
	   echo "</a>";
       loop_array($db, $array, $items->mlid);
       echo '</li>';
	 } 
     echo '</ul>';
  }
}

function display_menus($depth = 1) {
  $db = pg_connect("host=localhost port=5432 dbname=tripal2d7 user=tripal password=tri2pal");
  if (!$db) {
    echo "An error occurred to connect to database.\n";
    exit;
  }
  $result = pg_query($db, "select * from menu_links where menu_name='main-menu'  and hidden=0 and module='menu' order by weight asc");
  $array = array();
  while ($rows = pg_fetch_object($result)) {   
      $array[$rows->plid][] = $rows;
  }   
  loop_array($db, $array);
}
?>
