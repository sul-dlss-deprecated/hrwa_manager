require 'find'

class ImageDerivativeGenerator

  def self.printUsage()
    puts "\n" +
    'Usage:' + "\n" +
    "    image_derivative_generator [args...] file_or_directory_to_process<file or directory> output_directory<directory> new_width<integer> image_type_extension<String>" + "\n\n" +
    'Where options include:' + "\n" +
    '    --help' +
    "\n\n" +
    'Values for arguments:' + "\n" +
    '    file_or_directory_to_process' + "\n" +
    '        This can be a specific image file or a directory that contains image files.' + "\n" +
    '    output_directory' + "\n" +
    '        The output directory where generated derivatives will be saved.  The specified directory will be created if it does not already exist.' + "\n" +
    '    new_width' + "\n" +
    '        The new width of generated derivatives.' + "\n" +
    '    image_type_extension' + "\n" +
    "        An image type extension: 'png' or 'jpeg'. Other image formats might work, but this has only been tested with png and jpeg." + "\n" +
    "\n" +
    'System Requirement Info:' + "\n" +
    '    To use this script, you must have FreeImage installed on your system.' + "\n" +
    '    You also need to have the following gems:' + "\n" +
    "        - image_science, '~> 1.2.3'" +
    "\n\n"
  end

  def initialize(file_or_directory_to_process, output_directory, new_width, image_type_extension)

    if(file_or_directory_to_process.nil?)
      puts "\n----------------------------\n" +
      'Error: You must specify a file_or_directory_to_process.' +
      "\n----------------------------"
      ImageDerivativeGenerator::printUsage
      exit(0)
    end

    if(output_directory.nil?)
      puts "\n----------------------------\n" +
      'Error: You must specify an output_directory.' +
      "\n----------------------------"
      ImageDerivativeGenerator::printUsage
      exit(0)
    end

    if(new_width.nil?)
      puts "\n----------------------------\n" +
      'Error: You must specify a new width for your image derivatives.' +
      "\n----------------------------"
      ImageDerivativeGenerator::printUsage
      exit(0)
    end

    if(image_type_extension.nil?)
      puts "\n----------------------------\n" +
      'Error: You must specify an image file extension for your image derivatives.' +
      "\n----------------------------"
      ImageDerivativeGenerator::printUsage
      exit(0)
    end

    @output_dir = output_directory

    @new_width = new_width
    @image_type_extension = image_type_extension

    if(File.directory?(file_or_directory_to_process))
      puts 'Processing directory: ' + file_or_directory_to_process
      files_to_process = get_png_and_jpeg_files_in_directory(file_or_directory_to_process)
    else
      puts 'Processing file: ' + file_or_directory_to_process
      files_to_process = Array.new.push(file_or_directory_to_process)
    end

    process_files(files_to_process)

    puts 'Done!'
  end

  def process_files(arr_of_file_paths)

    if(arr_of_file_paths.length > 0)
      #Make @output_dir if it doesn't already exist
      pathname = Pathname.new(@output_dir)
      pathname.mkpath unless pathname.exist?
    end

    arr_of_file_paths.each { | file_path |

      puts 'Processing file: ' + file_path

      filename_without_extension = File.basename( file_path, ".*" )

      ImageScience.with_image(file_path) do |img|

        #img.cropped_thumbnail(100) do |thumb|
        #  thumb.save "#{@output_dir}/#{filename_without_extension}_cropped.jpeg"
        #end

        img.thumbnail(@new_width) do |thumb|
          thumb.save "#{@output_dir}/#{filename_without_extension}.#{@image_type_extension}"
        end

        #img.resize(100, 150) do |img2|
        #  img2.save "#{@output_dir}/#{filename_without_extension}_resize.jpeg"
        #end
      end
    }
  end

  def get_png_and_jpeg_files_in_directory(dir_path)

    arr_to_return = Array.new()

    basedir = File.expand_path(dir_path)

    puts 'Looking for image files in directory: ' + basedir

    Find.find(basedir.chomp) do |path|
      if FileTest.directory?(path)
        if File.basename(path)[0] == ?.
          Find.prune       # Don't look any further into this directory.
        else
          next
        end
      else
        if(is_jpeg_or_png_file_path(path))
          arr_to_return.push(path)
        end
      end
    end

    return arr_to_return

  end

  def is_jpeg_or_png_file_path(file_string)
    return /\.(jpg|jpeg|png)$/i.match( file_string )
  end

end
