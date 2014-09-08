/* Function for comparing the annotation track names for sorting in the
   selection table.  You can customize this to provide your own sorting
   function.  Note that the function must be called "track_name_comparator"
   and return -1 for "less than", 0 for equal, and 1 for "greater than".
*/
function track_name_comparator(a, b) {
	return a < b ? -1 : a > b ? 1 : 0;
};
